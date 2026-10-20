package com.tourhive.backend.services.admin.impl;

import com.tourhive.backend.dto.admin.request.BlockUserRequestDto;
import com.tourhive.backend.dto.admin.request.UpdateUserRoleRequestDto;
import com.tourhive.backend.dto.admin.response.AdminUserDetailResponseDto;
import com.tourhive.backend.dto.admin.response.AdminUserResponseDto;
import com.tourhive.backend.entities.audit.AuditAction;
import com.tourhive.backend.entities.reports.ReportTargetType;
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
import com.tourhive.backend.services.admin.AdminUserService;
import com.tourhive.backend.services.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final ReportRepository reportRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDto> getUsers(String search, Role role, UserStatus status, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<User> page = (cleanSearch != null)
                ? userRepository.findUsersFilteredWithSearch(cleanSearch, role, status, pageable)
                : userRepository.findUsersFilteredWithoutSearch(role, status, pageable);
        return page.map(this::mapToAdminUserResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponseDto getUserDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        long totalToursCreated = tourRepository.countByOrganizerId(userId);
        long totalApplicationsSubmitted = tourApplicationRepository.countByApplicantId(userId);
        long totalReportsSubmitted = reportRepository.countByReporterId(userId);
        long totalReportsReceived = reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, userId);

        return new AdminUserDetailResponseDto(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getCountry() : null,
                profile != null ? profile.getCity() : null,
                profile != null ? profile.getPhone() : null,
                user.getRole(),
                user.getStatus(),
                user.isOrganizer(),
                user.isBusinessVerified(),
                user.getBlockReason(),
                totalToursCreated,
                totalApplicationsSubmitted,
                totalReportsSubmitted,
                totalReportsReceived,
                user.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public AdminUserResponseDto blockUser(UUID adminId, String adminEmail, UUID userId, BlockUserRequestDto request, String ipAddress) {
        if (adminId.equals(userId)) {
            throw new CannotModifySelfAdminException("You cannot block your own administrative account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.setStatus(UserStatus.BLOCKED);
        user.setBlockReason(request.reason());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        auditService.logAction(
                adminId,
                adminEmail,
                AuditAction.USER_BLOCKED,
                "USER",
                userId,
                "Reason: " + request.reason(),
                ipAddress
        );

        log.info("User {} blocked by admin {} with reason: {}", userId, adminEmail, request.reason());
        return mapToAdminUserResponseDto(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto unblockUser(UUID adminId, String adminEmail, UUID userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.setStatus(UserStatus.ACTIVE);
        user.setBlockReason(null);
        userRepository.save(user);

        auditService.logAction(
                adminId,
                adminEmail,
                AuditAction.USER_UNBLOCKED,
                "USER",
                userId,
                "Account unblocked",
                ipAddress
        );

        log.info("User {} unblocked by admin {}", userId, adminEmail);
        return mapToAdminUserResponseDto(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDto updateUserRole(UUID adminId, String adminEmail, UUID userId, UpdateUserRoleRequestDto request, String ipAddress) {
        if (adminId.equals(userId)) {
            throw new CannotModifySelfAdminException("You cannot modify your own administrative role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        Role oldRole = user.getRole();
        user.setRole(request.role());
        userRepository.save(user);

        auditService.logAction(
                adminId,
                adminEmail,
                AuditAction.USER_ROLE_CHANGED,
                "USER",
                userId,
                "Old Role: " + oldRole + ", New Role: " + request.role(),
                ipAddress
        );

        log.info("User {} role changed from {} to {} by admin {}", userId, oldRole, request.role(), adminEmail);
        return mapToAdminUserResponseDto(user);
    }

    private AdminUserResponseDto mapToAdminUserResponseDto(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return new AdminUserResponseDto(
                user.getId(),
                user.getEmail(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                user.getRole(),
                user.getStatus(),
                user.isOrganizer(),
                user.isBusinessVerified(),
                user.getBlockReason(),
                user.getCreatedAt()
        );
    }
}
