package com.tourhive.backend.admin;

import com.tourhive.backend.dto.admin.request.BlockUserRequestDto;
import com.tourhive.backend.dto.admin.request.UpdateUserRoleRequestDto;
import com.tourhive.backend.dto.admin.response.AdminUserDetailResponseDto;
import com.tourhive.backend.dto.admin.response.AdminUserResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.CannotModifySelfAdminException;
import com.tourhive.backend.exceptions.UserNotFoundException;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.repositories.reports.ReportRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.admin.impl.AdminUserServiceImpl;
import com.tourhive.backend.services.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private UUID adminId;
    private UUID targetUserId;
    private User targetUser;
    private UserProfile targetProfile;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        targetUser = User.builder()
                .email("user@example.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        targetUser.setId(targetUserId);

        targetProfile = UserProfile.builder()
                .user(targetUser)
                .nickname("traveler_bob")
                .firstName("Bob")
                .lastName("Traveler")
                .build();
    }

    @Test
    @DisplayName("Successfully block user, revoke tokens, and log audit")
    void blockUserSuccess() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetProfile));

        BlockUserRequestDto request = new BlockUserRequestDto("Violation of guidelines");
        AdminUserResponseDto response = adminUserService.blockUser(adminId, "admin@tourhive.com", targetUserId, request, "127.0.0.1");

        assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
        assertThat(response.blockReason()).isEqualTo("Violation of guidelines");
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(refreshTokenRepository).revokeAllByUserId(targetUserId);
        verify(auditService).logAction(eq(adminId), eq("admin@tourhive.com"), eq(AuditAction.USER_BLOCKED), eq("USER"), eq(targetUserId), any(), any());
    }

    @Test
    @DisplayName("Should throw CannotModifySelfAdminException when admin attempts to block self")
    void blockSelfThrowsException() {
        BlockUserRequestDto request = new BlockUserRequestDto("Self block");

        assertThatThrownBy(() -> adminUserService.blockUser(adminId, "admin@tourhive.com", adminId, request, "127.0.0.1"))
                .isInstanceOf(CannotModifySelfAdminException.class);
    }

    @Test
    @DisplayName("Successfully unblock user and log audit")
    void unblockUserSuccess() {
        targetUser.setStatus(UserStatus.BLOCKED);
        targetUser.setBlockReason("Past violation");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetProfile));

        AdminUserResponseDto response = adminUserService.unblockUser(adminId, "admin@tourhive.com", targetUserId, "127.0.0.1");

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.blockReason()).isNull();
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(auditService).logAction(eq(adminId), eq("admin@tourhive.com"), eq(AuditAction.USER_UNBLOCKED), eq("USER"), eq(targetUserId), any(), any());
    }

    @Test
    @DisplayName("Successfully update user role and log audit")
    void updateUserRoleSuccess() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetProfile));

        UpdateUserRoleRequestDto request = new UpdateUserRoleRequestDto(Role.MODERATOR);
        AdminUserResponseDto response = adminUserService.updateUserRole(adminId, "admin@tourhive.com", targetUserId, request, "127.0.0.1");

        assertThat(response.role()).isEqualTo(Role.MODERATOR);
        assertThat(targetUser.getRole()).isEqualTo(Role.MODERATOR);
        verify(auditService).logAction(eq(adminId), eq("admin@tourhive.com"), eq(AuditAction.USER_ROLE_CHANGED), eq("USER"), eq(targetUserId), any(), any());
    }

    @Test
    @DisplayName("Should throw CannotModifySelfAdminException when admin attempts to change own role")
    void changeSelfRoleThrowsException() {
        UpdateUserRoleRequestDto request = new UpdateUserRoleRequestDto(Role.USER);

        assertThatThrownBy(() -> adminUserService.updateUserRole(adminId, "admin@tourhive.com", adminId, request, "127.0.0.1"))
                .isInstanceOf(CannotModifySelfAdminException.class);
    }

    @Test
    @DisplayName("Get user details returns aggregated statistics")
    void getUserDetailsReturnsStatistics() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userProfileRepository.findByUserId(targetUserId)).thenReturn(Optional.of(targetProfile));
        when(tourRepository.countByOrganizerId(targetUserId)).thenReturn(3L);
        when(tourApplicationRepository.countByApplicantId(targetUserId)).thenReturn(7L);
        when(reportRepository.countByReporterId(targetUserId)).thenReturn(1L);
        when(reportRepository.countByTargetTypeAndTargetId(any(), eq(targetUserId))).thenReturn(0L);

        AdminUserDetailResponseDto details = adminUserService.getUserDetails(targetUserId);

        assertThat(details).isNotNull();
        assertThat(details.nickname()).isEqualTo("traveler_bob");
        assertThat(details.totalToursCreated()).isEqualTo(3L);
        assertThat(details.totalApplicationsSubmitted()).isEqualTo(7L);
        assertThat(details.totalReportsSubmitted()).isEqualTo(1L);
        assertThat(details.totalReportsReceived()).isEqualTo(0L);
    }
}
