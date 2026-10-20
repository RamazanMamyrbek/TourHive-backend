package com.tourhive.backend.tours;

import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.CancelApplicationRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.dto.tours.response.TourApplicationResponseDto;
import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.ApplicationAccessDeniedException;
import com.tourhive.backend.exceptions.DuplicateApplicationException;
import com.tourhive.backend.exceptions.InvalidApplicationStateException;
import com.tourhive.backend.exceptions.TourCapacityExceededException;
import com.tourhive.backend.mappers.tours.TourApplicationMapper;
import com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.tours.impl.TourApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourApplicationServiceTest {

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private com.tourhive.backend.services.chats.ChatService chatService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Spy
    private TourApplicationMapper tourApplicationMapper = Mappers.getMapper(TourApplicationMapper.class);

    @InjectMocks
    private TourApplicationServiceImpl tourApplicationService;

    private User organizer;
    private User traveler;
    private Tour tour;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .email("organizer@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .build();
        organizer.setId(UUID.randomUUID());

        traveler = User.builder()
                .email("traveler@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build();
        traveler.setId(UUID.randomUUID());

        tour = Tour.builder()
                .organizer(organizer)
                .title("Kolsay Lake Hiking")
                .description("Mountain hiking")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(14))
                .capacity(5)
                .availablePlaces(5)
                .price(BigDecimal.valueOf(15000))
                .build();
        tour.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("should submit application successfully")
    void shouldApplyForTourSuccessfully() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(userRepository.findById(traveler.getId())).thenReturn(Optional.of(traveler));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(tourApplicationRepository.save(any(TourApplication.class))).thenAnswer(i -> {
            TourApplication app = i.getArgument(0);
            app.setId(UUID.randomUUID());
            return app;
        });

        TourApplicationResponseDto result = tourApplicationService.applyForTour(
                tour.getId(),
                traveler.getId(),
                new ApplyTourRequestDto("Ready to hike!", "+77011234567")
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourApplicationStatus.PENDING);
        assertThat(result.message()).isEqualTo("Ready to hike!");
        assertThat(result.contactInfo()).isEqualTo("+77011234567");
        verify(applicationStatusHistoryRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    @DisplayName("should fail applying to own tour")
    void shouldFailApplyingToOwnTour() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourApplicationService.applyForTour(
                tour.getId(),
                organizer.getId(),
                new ApplyTourRequestDto("Self application", null)
        )).isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("Organizers cannot apply to their own tours");
    }

    @Test
    @DisplayName("should fail applying to non-published tour")
    void shouldFailApplyingToNonPublishedTour() {
        tour.setStatus(TourStatus.DRAFT);
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourApplicationService.applyForTour(
                tour.getId(),
                traveler.getId(),
                new ApplyTourRequestDto("Join", null)
        )).isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("Applications can only be submitted for published tours");
    }

    @Test
    @DisplayName("should fail if active application already exists")
    void shouldFailIfDuplicateApplication() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(userRepository.findById(traveler.getId())).thenReturn(Optional.of(traveler));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> tourApplicationService.applyForTour(
                tour.getId(),
                traveler.getId(),
                new ApplyTourRequestDto("Join again", null)
        )).isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("already have an active application");
    }

    @Test
    @DisplayName("organizer accepts application -> decrements available places")
    void shouldAcceptApplicationAndDecrementAvailablePlaces() {
        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .build();
        application.setId(UUID.randomUUID());

        when(tourRepository.findByIdWithPessimisticLock(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        when(tourApplicationRepository.save(any(TourApplication.class))).thenAnswer(i -> i.getArgument(0));

        TourApplicationResponseDto result = tourApplicationService.reviewApplication(
                tour.getId(),
                application.getId(),
                organizer.getId(),
                new ReviewApplicationRequestDto(TourApplicationStatus.ACCEPTED, null)
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourApplicationStatus.ACCEPTED);
        assertThat(tour.getAvailablePlaces()).isEqualTo(4); // was 5
        verify(tourRepository).save(tour);
        verify(applicationStatusHistoryRepository).save(any(ApplicationStatusHistory.class));
    }

    @Test
    @DisplayName("organizer accepting application when capacity is 0 fails with TourCapacityExceededException")
    void shouldFailAcceptingWhenCapacityExceeded() {
        tour.setAvailablePlaces(0);

        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .build();
        application.setId(UUID.randomUUID());

        when(tourRepository.findByIdWithPessimisticLock(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));

        assertThatThrownBy(() -> tourApplicationService.reviewApplication(
                tour.getId(),
                application.getId(),
                organizer.getId(),
                new ReviewApplicationRequestDto(TourApplicationStatus.ACCEPTED, null)
        )).isInstanceOf(TourCapacityExceededException.class)
                .hasMessageContaining("No available places left in this tour");
    }

    @Test
    @DisplayName("organizer rejects application with reason -> leaves available places unchanged")
    void shouldRejectApplicationWithReason() {
        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .build();
        application.setId(UUID.randomUUID());

        when(tourRepository.findByIdWithPessimisticLock(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        when(tourApplicationRepository.save(any(TourApplication.class))).thenAnswer(i -> i.getArgument(0));

        TourApplicationResponseDto result = tourApplicationService.reviewApplication(
                tour.getId(),
                application.getId(),
                organizer.getId(),
                new ReviewApplicationRequestDto(TourApplicationStatus.REJECTED, "Group requires high altitude certification")
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourApplicationStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo("Group requires high altitude certification");
        assertThat(tour.getAvailablePlaces()).isEqualTo(5); // unchanged
    }

    @Test
    @DisplayName("traveler withdraws pending application")
    void shouldWithdrawPendingApplication() {
        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.PENDING)
                .build();
        application.setId(UUID.randomUUID());

        when(tourRepository.findByIdWithPessimisticLock(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(userRepository.findById(traveler.getId())).thenReturn(Optional.of(traveler));
        when(tourApplicationRepository.save(any(TourApplication.class))).thenAnswer(i -> i.getArgument(0));

        TourApplicationResponseDto result = tourApplicationService.cancelApplication(
                tour.getId(),
                application.getId(),
                traveler.getId(),
                new CancelApplicationRequestDto("Changed my mind")
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourApplicationStatus.WITHDRAWN);
        assertThat(result.cancellationReason()).isEqualTo("Changed my mind");
        assertThat(tour.getAvailablePlaces()).isEqualTo(5); // unchanged
    }

    @Test
    @DisplayName("traveler cancels accepted participation -> increments available places")
    void shouldCancelAcceptedParticipationAndIncrementAvailablePlaces() {
        tour.setAvailablePlaces(2); // 2 of 5 available

        TourApplication application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.ACCEPTED)
                .build();
        application.setId(UUID.randomUUID());

        when(tourRepository.findByIdWithPessimisticLock(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(userRepository.findById(traveler.getId())).thenReturn(Optional.of(traveler));
        when(tourApplicationRepository.save(any(TourApplication.class))).thenAnswer(i -> i.getArgument(0));

        TourApplicationResponseDto result = tourApplicationService.cancelApplication(
                tour.getId(),
                application.getId(),
                traveler.getId(),
                new CancelApplicationRequestDto("Work schedule conflict")
        );

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TourApplicationStatus.CANCELLED_BY_TRAVELER);
        assertThat(tour.getAvailablePlaces()).isEqualTo(3); // restored place (was 2, now 3)
        verify(tourRepository).save(tour);
    }
}
