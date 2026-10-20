package com.tourhive.backend.services.reviews.impl;

import com.tourhive.backend.dto.reviews.request.CreateReviewRequestDto;
import com.tourhive.backend.dto.reviews.request.UpdateReviewRequestDto;
import com.tourhive.backend.dto.reviews.response.ReviewResponseDto;
import com.tourhive.backend.entities.notifications.NotificationType;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import com.tourhive.backend.entities.reviews.Review;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.events.NotificationEvent;
import com.tourhive.backend.exceptions.CannotReviewOwnTourException;
import com.tourhive.backend.exceptions.DuplicateReviewException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.ReviewAccessDeniedException;
import com.tourhive.backend.exceptions.ReviewNotFoundException;
import com.tourhive.backend.exceptions.TourCancelledException;
import com.tourhive.backend.exceptions.TourNotCompletedException;
import com.tourhive.backend.mappers.reviews.ReviewMapper;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.reviews.ReviewRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.reviews.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReviewResponseDto createReview(UUID tourId, UUID authorId, CreateReviewRequestDto requestDto) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));

        if (tour.getStatus() == TourStatus.CANCELLED) {
            throw new TourCancelledException("Cannot review a cancelled tour");
        }

        if (tour.getEndDate() != null && tour.getEndDate().isAfter(LocalDate.now())) {
            throw new TourNotCompletedException("Cannot review a tour that has not ended yet. End date is " + tour.getEndDate());
        }

        if (tour.getOrganizer().getId().equals(authorId)) {
            throw new CannotReviewOwnTourException("Organizers cannot review their own tours");
        }

        boolean isAcceptedParticipant = tourApplicationRepository.existsByTourIdAndApplicantIdAndStatus(
                tourId, authorId, TourApplicationStatus.ACCEPTED);
        if (!isAcceptedParticipant) {
            throw new ReviewAccessDeniedException("Only accepted participants can review this tour");
        }

        if (reviewRepository.existsByTourIdAndAuthorId(tourId, authorId)) {
            throw new DuplicateReviewException("You have already reviewed this tour");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author user not found: " + authorId));

        Review review = Review.builder()
                .tour(tour)
                .organizer(tour.getOrganizer())
                .author(author)
                .rating(requestDto.rating())
                .comment(requestDto.comment() != null ? requestDto.comment().trim() : null)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Saved review [id={}] for tour {} by author {}", savedReview.getId(), tourId, authorId);

        recalculateOrganizerRating(tour.getOrganizer().getId());

        eventPublisher.publishEvent(new NotificationEvent(
                tour.getOrganizer().getId(),
                NotificationType.TOUR_REVIEWED,
                "New Tour Review",
                "A participant rated your tour '" + tour.getTitle() + "' with " + savedReview.getRating() + " stars.",
                "REVIEW",
                savedReview.getId()
        ));

        UserProfile authorProfile = userProfileRepository.findByUserId(authorId).orElse(null);
        return reviewMapper.toDto(savedReview, authorProfile);
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(UUID reviewId, UUID authorId, UpdateReviewRequestDto requestDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewId));

        if (!review.getAuthor().getId().equals(authorId)) {
            throw new ReviewAccessDeniedException("You can only edit your own review");
        }

        review.setRating(requestDto.rating());
        review.setComment(requestDto.comment() != null ? requestDto.comment().trim() : null);

        Review updated = reviewRepository.save(review);
        log.info("Updated review [id={}] by author {}", reviewId, authorId);

        recalculateOrganizerRating(review.getOrganizer().getId());

        UserProfile authorProfile = userProfileRepository.findByUserId(authorId).orElse(null);
        return reviewMapper.toDto(updated, authorProfile);
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId, UUID currentUserId, boolean isModerator) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found: " + reviewId));

        if (!review.getAuthor().getId().equals(currentUserId) && !isModerator) {
            throw new ReviewAccessDeniedException("You do not have permission to delete this review");
        }

        UUID organizerId = review.getOrganizer().getId();
        reviewRepository.delete(review);
        log.info("Deleted review [id={}] by user {}", reviewId, currentUserId);

        recalculateOrganizerRating(organizerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getTourReviews(UUID tourId, Pageable pageable) {
        tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));

        Page<Review> reviews = reviewRepository.findAllByTourIdOrderByCreatedAtDesc(tourId, pageable);
        return reviews.map(r -> {
            UserProfile p = userProfileRepository.findByUserId(r.getAuthor().getId()).orElse(null);
            return reviewMapper.toDto(r, p);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getOrganizerReviews(UUID organizerId, Pageable pageable) {
        userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer user not found: " + organizerId));

        Page<Review> reviews = reviewRepository.findAllByOrganizerIdOrderByCreatedAtDesc(organizerId, pageable);
        return reviews.map(r -> {
            UserProfile p = userProfileRepository.findByUserId(r.getAuthor().getId()).orElse(null);
            return reviewMapper.toDto(r, p);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getMyReviews(UUID authorId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        UserProfile authorProfile = userProfileRepository.findByUserId(authorId).orElse(null);
        return reviews.map(r -> reviewMapper.toDto(r, authorProfile));
    }

    private void recalculateOrganizerRating(UUID organizerId) {
        Double avg = reviewRepository.calculateAverageRatingForOrganizer(organizerId);
        int count = reviewRepository.countReviewsForOrganizer(organizerId);

        BigDecimal rating = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        organizerProfileRepository.findByUserId(organizerId).ifPresent(profile -> {
            profile.setRating(rating);
            profile.setReviewsCount(count);
            organizerProfileRepository.save(profile);
            log.info("Updated organizer {} rating to {} based on {} reviews", organizerId, rating, count);
        });
    }
}
