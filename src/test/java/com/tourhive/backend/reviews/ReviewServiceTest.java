package com.tourhive.backend.reviews;

import com.tourhive.backend.dto.reviews.request.CreateReviewRequestDto;
import com.tourhive.backend.dto.reviews.request.UpdateReviewRequestDto;
import com.tourhive.backend.dto.reviews.response.ReviewResponseDto;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.reviews.Review;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CannotReviewOwnTourException;
import com.tourhive.backend.exceptions.DuplicateReviewException;
import com.tourhive.backend.exceptions.ReviewAccessDeniedException;
import com.tourhive.backend.exceptions.TourCancelledException;
import com.tourhive.backend.exceptions.TourNotCompletedException;
import com.tourhive.backend.mappers.reviews.ReviewMapper;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.reviews.ReviewRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.reviews.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourApplicationRepository tourApplicationRepository;

    @Mock
    private OrganizerProfileRepository organizerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ReviewMapper reviewMapper = Mappers.getMapper(ReviewMapper.class);

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User organizer;
    private User traveler;
    private Tour tour;
    private TourApplication application;
    private OrganizerProfile organizerProfile;

    @BeforeEach
    void setUp() {
        organizer = User.builder().email("organizer@tourhive.com").build();
        organizer.setId(UUID.randomUUID());

        traveler = User.builder().email("traveler@tourhive.com").build();
        traveler.setId(UUID.randomUUID());

        tour = Tour.builder()
                .organizer(organizer)
                .title("Tian Shan Expedition")
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(2))
                .build();
        tour.setId(UUID.randomUUID());

        application = TourApplication.builder()
                .tour(tour)
                .applicant(traveler)
                .status(TourApplicationStatus.ACCEPTED)
                .build();
        application.setId(UUID.randomUUID());

        organizerProfile = OrganizerProfile.builder()
                .user(organizer)
                .rating(BigDecimal.ZERO)
                .reviewsCount(0)
                .build();
    }

    @Test
    @DisplayName("Successfully create review for completed tour by accepted participant")
    void createReviewSuccess() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(tour.getId(), traveler.getId(), TourApplicationStatus.ACCEPTED)).thenReturn(true);
        when(reviewRepository.existsByTourIdAndAuthorId(tour.getId(), traveler.getId())).thenReturn(false);
        when(userRepository.findById(traveler.getId())).thenReturn(Optional.of(traveler));
        when(userProfileRepository.findByUserId(traveler.getId())).thenReturn(Optional.of(UserProfile.builder().user(traveler).firstName("Alice").build()));

        Review savedReview = Review.builder()
                .tour(tour)
                .organizer(organizer)
                .author(traveler)
                .rating(5)
                .comment("Awesome tour!")
                .build();
        savedReview.setId(UUID.randomUUID());

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.calculateAverageRatingForOrganizer(organizer.getId())).thenReturn(5.0);
        when(reviewRepository.countReviewsForOrganizer(organizer.getId())).thenReturn(1);
        when(organizerProfileRepository.findByUserId(organizer.getId())).thenReturn(Optional.of(organizerProfile));

        ReviewResponseDto result = reviewService.createReview(tour.getId(), traveler.getId(), new CreateReviewRequestDto(5, "Awesome tour!"));

        assertThat(result).isNotNull();
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.comment()).isEqualTo("Awesome tour!");
        verify(organizerProfileRepository).save(organizerProfile);
        verify(eventPublisher).publishEvent(any(com.tourhive.backend.events.NotificationEvent.class));
    }

    @Test
    @DisplayName("Cannot review tour that is not completed yet")
    void cannotReviewUnfinishedTour() {
        tour.setEndDate(LocalDate.now().plusDays(5));
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> reviewService.createReview(tour.getId(), traveler.getId(), new CreateReviewRequestDto(5, "Good")))
                .isInstanceOf(TourNotCompletedException.class);
    }

    @Test
    @DisplayName("Cannot review cancelled tour")
    void cannotReviewCancelledTour() {
        tour.setStatus(TourStatus.CANCELLED);
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> reviewService.createReview(tour.getId(), traveler.getId(), new CreateReviewRequestDto(5, "Good")))
                .isInstanceOf(TourCancelledException.class);
    }

    @Test
    @DisplayName("Organizer cannot review own tour")
    void cannotReviewOwnTour() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> reviewService.createReview(tour.getId(), organizer.getId(), new CreateReviewRequestDto(5, "Good")))
                .isInstanceOf(CannotReviewOwnTourException.class);
    }

    @Test
    @DisplayName("User without accepted participation cannot review tour")
    void unacceptedParticipantCannotReview() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(tour.getId(), traveler.getId(), TourApplicationStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(tour.getId(), traveler.getId(), new CreateReviewRequestDto(5, "Good")))
                .isInstanceOf(ReviewAccessDeniedException.class);
    }

    @Test
    @DisplayName("Duplicate review on same tour is rejected with Conflict")
    void duplicateReviewRejected() {
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(tour.getId(), traveler.getId(), TourApplicationStatus.ACCEPTED)).thenReturn(true);
        when(reviewRepository.existsByTourIdAndAuthorId(tour.getId(), traveler.getId())).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(tour.getId(), traveler.getId(), new CreateReviewRequestDto(5, "Good")))
                .isInstanceOf(DuplicateReviewException.class);
    }

    @Test
    @DisplayName("Author can update review rating and comment")
    void updateReviewSuccess() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().tour(tour).organizer(organizer).author(traveler).rating(4).comment("Initial").build();
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.calculateAverageRatingForOrganizer(organizer.getId())).thenReturn(5.0);
        when(reviewRepository.countReviewsForOrganizer(organizer.getId())).thenReturn(1);
        when(organizerProfileRepository.findByUserId(organizer.getId())).thenReturn(Optional.of(organizerProfile));
        when(userProfileRepository.findByUserId(traveler.getId())).thenReturn(Optional.empty());

        ReviewResponseDto result = reviewService.updateReview(reviewId, traveler.getId(), new UpdateReviewRequestDto(5, "Updated comment"));

        assertThat(result).isNotNull();
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Updated comment");
    }

    @Test
    @DisplayName("Author can delete review and recalculate organizer rating")
    void deleteReviewSuccess() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder().tour(tour).organizer(organizer).author(traveler).rating(4).comment("To delete").build();
        review.setId(reviewId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageRatingForOrganizer(organizer.getId())).thenReturn(0.0);
        when(reviewRepository.countReviewsForOrganizer(organizer.getId())).thenReturn(0);
        when(organizerProfileRepository.findByUserId(organizer.getId())).thenReturn(Optional.of(organizerProfile));

        reviewService.deleteReview(reviewId, traveler.getId(), false);

        verify(reviewRepository).delete(review);
        verify(organizerProfileRepository).save(organizerProfile);
    }

    @Test
    @DisplayName("Get reviews for tour")
    void getTourReviews() {
        Review review = Review.builder().tour(tour).organizer(organizer).author(traveler).rating(5).comment("Nice").build();
        when(tourRepository.findById(tour.getId())).thenReturn(Optional.of(tour));
        when(reviewRepository.findAllByTourIdOrderByCreatedAtDesc(tour.getId(), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(userProfileRepository.findByUserId(traveler.getId())).thenReturn(Optional.empty());

        Page<ReviewResponseDto> page = reviewService.getTourReviews(tour.getId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }
}
