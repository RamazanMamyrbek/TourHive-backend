package com.tourhive.backend.admin;

import com.tourhive.backend.dto.admin.request.ReviewTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.InvalidTourDataException;
import com.tourhive.backend.exceptions.TourNotFoundException;
import com.tourhive.backend.mappers.tours.TourMapper;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.admin.impl.AdminTourServiceImpl;
import com.tourhive.backend.services.audit.AuditService;
import com.tourhive.backend.services.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Spy
    private TourMapper tourMapper = Mappers.getMapper(TourMapper.class);

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminTourServiceImpl adminTourService;

    private User moderator;
    private User organizer;
    private Tour pendingTour;

    @BeforeEach
    void setUp() {
        moderator = User.builder()
                .email("mod@tourhive.com")
                .passwordHash("hash")
                .role(Role.MODERATOR)
                .status(UserStatus.ACTIVE)
                .build();
        moderator.setId(UUID.randomUUID());

        organizer = User.builder()
                .email("org@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .build();
        organizer.setId(UUID.randomUUID());

        pendingTour = Tour.builder()
                .organizer(organizer)
                .title("Big Almaty Lake Trek")
                .status(TourStatus.PENDING_REVIEW)
                .tourType(TourType.COMMUNITY)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .capacity(15)
                .build();
        pendingTour.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("should get pending tours page")
    void shouldGetPendingTours() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(tourRepository.findAll(any(Specification.class), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(pendingTour)));

        Page<TourResponseDto> result = adminTourService.getPendingTours(pageRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Big Almaty Lake Trek");
    }

    @Test
    @DisplayName("should approve tour to PUBLISHED status")
    void shouldApproveTour() {
        when(tourRepository.findById(pendingTour.getId())).thenReturn(Optional.of(pendingTour));
        when(userService.getById(moderator.getId())).thenReturn(moderator);
        when(tourRepository.save(any(Tour.class))).thenAnswer(i -> i.getArgument(0));

        ReviewTourRequestDto reviewDto = new ReviewTourRequestDto(TourStatus.PUBLISHED, null);
        TourResponseDto result = adminTourService.reviewTour(moderator.getId(), pendingTour.getId(), reviewDto);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourStatus.PUBLISHED);
        assertThat(pendingTour.getStatus()).isEqualTo(TourStatus.PUBLISHED);
        assertThat(pendingTour.getReviewedBy()).isEqualTo(moderator);
        assertThat(pendingTour.getPublishedAt()).isNotNull();
        assertThat(pendingTour.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("should reject tour with reason")
    void shouldRejectTour() {
        when(tourRepository.findById(pendingTour.getId())).thenReturn(Optional.of(pendingTour));
        when(userService.getById(moderator.getId())).thenReturn(moderator);
        when(tourRepository.save(any(Tour.class))).thenAnswer(i -> i.getArgument(0));

        ReviewTourRequestDto reviewDto = new ReviewTourRequestDto(TourStatus.REJECTED, "Low quality photos");
        TourResponseDto result = adminTourService.reviewTour(moderator.getId(), pendingTour.getId(), reviewDto);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourStatus.REJECTED);
        assertThat(pendingTour.getStatus()).isEqualTo(TourStatus.REJECTED);
        assertThat(pendingTour.getRejectionReason()).isEqualTo("Low quality photos");
    }

    @Test
    @DisplayName("should throw when rejecting tour without reason")
    void shouldThrowWhenRejectingWithoutReason() {
        when(tourRepository.findById(pendingTour.getId())).thenReturn(Optional.of(pendingTour));

        ReviewTourRequestDto reviewDto = new ReviewTourRequestDto(TourStatus.REJECTED, null);

        assertThatThrownBy(() -> adminTourService.reviewTour(moderator.getId(), pendingTour.getId(), reviewDto))
                .isInstanceOf(InvalidTourDataException.class)
                .hasMessageContaining("Rejection reason is required");
    }
}
