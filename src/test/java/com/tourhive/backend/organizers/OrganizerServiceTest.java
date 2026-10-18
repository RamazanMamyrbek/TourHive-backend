package com.tourhive.backend.organizers;

import com.tourhive.backend.dto.organizers.request.BusinessDocumentRequestDto;
import com.tourhive.backend.dto.organizers.request.RegisterBusinessProfileRequestDto;
import com.tourhive.backend.dto.organizers.response.BusinessProfileResponseDto;
import com.tourhive.backend.dto.organizers.response.OrganizerProfileResponseDto;
import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.exceptions.BusinessProfileAlreadyExistsException;
import com.tourhive.backend.mappers.organizers.OrganizerMapper;
import com.tourhive.backend.repositories.organizers.BusinessProfileRepository;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.organizers.impl.OrganizerServiceImpl;
import com.tourhive.backend.services.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class OrganizerServiceTest {

    @Mock
    private OrganizerProfileRepository organizerProfileRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private OrganizerMapper organizerMapper;

    @InjectMocks
    private OrganizerServiceImpl organizerService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().email("guide@tourhive.com").isOrganizer(false).build();
        user.setId(userId);
    }

    @Test
    @DisplayName("Should activate organizer status and create organizer profile")
    void shouldActivateOrganizer() {
        when(userService.getById(userId)).thenReturn(user);
        when(organizerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        OrganizerProfile createdProfile = OrganizerProfile.builder()
                .user(user)
                .experienceYears(0)
                .rating(BigDecimal.ZERO)
                .reviewsCount(0)
                .build();
        when(organizerProfileRepository.save(any(OrganizerProfile.class))).thenReturn(createdProfile);
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        OrganizerProfileResponseDto expectedDto = new OrganizerProfileResponseDto(
                UUID.randomUUID(),
                userId,
                null,
                0,
                BigDecimal.ZERO,
                0,
                true,
                false,
                null
        );
        when(organizerMapper.toOrganizerProfileDto(any(), eq(null))).thenReturn(expectedDto);

        OrganizerProfileResponseDto result = organizerService.activateOrganizer(userId);

        assertThat(user.isOrganizer()).isTrue();
        verify(userRepository).save(user);
        verify(organizerProfileRepository).save(any(OrganizerProfile.class));
        assertThat(result.isOrganizer()).isTrue();
    }

    @Test
    @DisplayName("Should throw BusinessProfileAlreadyExistsException if pending or verified application exists")
    void shouldThrowIfActiveBusinessProfileExists() {
        when(userService.getById(userId)).thenReturn(user);
        when(businessProfileRepository.existsByUserIdAndStatusIn(eq(userId), any())).thenReturn(true);

        RegisterBusinessProfileRequestDto request = new RegisterBusinessProfileRequestDto(
                "Almaty Tours LLC",
                "BIN1234567890",
                "Kazakhstan",
                "contact@almatytours.kz",
                "+77011234567",
                "Tour Operator",
                List.of(new BusinessDocumentRequestDto("License", "https://minio.local/doc.pdf"))
        );

        assertThatThrownBy(() -> organizerService.registerBusinessProfile(userId, request))
                .isInstanceOf(BusinessProfileAlreadyExistsException.class)
                .hasMessageContaining("Active or pending business profile already exists");
    }

    @Test
    @DisplayName("Should successfully submit business profile application")
    void shouldRegisterBusinessProfile() {
        when(userService.getById(userId)).thenReturn(user);
        when(businessProfileRepository.existsByUserIdAndStatusIn(eq(userId), any())).thenReturn(false);
        when(organizerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(businessProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        BusinessProfile savedProfile = BusinessProfile.builder()
                .user(user)
                .legalName("Almaty Tours LLC")
                .registrationNumber("BIN1234567890")
                .country("Kazakhstan")
                .contactEmail("contact@almatytours.kz")
                .contactPhone("+77011234567")
                .description("Tour Operator")
                .status(BusinessVerificationStatus.PENDING)
                .build();
        savedProfile.setId(UUID.randomUUID());

        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(savedProfile);

        BusinessProfileResponseDto expectedDto = new BusinessProfileResponseDto(
                savedProfile.getId(),
                "Almaty Tours LLC",
                "BIN1234567890",
                "Kazakhstan",
                "contact@almatytours.kz",
                "+77011234567",
                "Tour Operator",
                BusinessVerificationStatus.PENDING,
                null,
                List.of(),
                null
        );
        when(organizerMapper.toBusinessProfileDto(savedProfile)).thenReturn(expectedDto);

        RegisterBusinessProfileRequestDto request = new RegisterBusinessProfileRequestDto(
                "Almaty Tours LLC",
                "BIN1234567890",
                "Kazakhstan",
                "contact@almatytours.kz",
                "+77011234567",
                "Tour Operator",
                List.of(new BusinessDocumentRequestDto("License", "https://minio.local/doc.pdf"))
        );

        BusinessProfileResponseDto result = organizerService.registerBusinessProfile(userId, request);

        assertThat(result.status()).isEqualTo(BusinessVerificationStatus.PENDING);
        assertThat(result.legalName()).isEqualTo("Almaty Tours LLC");
        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }
}
