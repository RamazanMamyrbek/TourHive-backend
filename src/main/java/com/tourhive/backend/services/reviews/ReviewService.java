package com.tourhive.backend.services.reviews;

import com.tourhive.backend.dto.reviews.request.CreateReviewRequestDto;
import com.tourhive.backend.dto.reviews.request.UpdateReviewRequestDto;
import com.tourhive.backend.dto.reviews.response.ReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponseDto createReview(UUID tourId, UUID authorId, CreateReviewRequestDto requestDto);

    ReviewResponseDto updateReview(UUID reviewId, UUID authorId, UpdateReviewRequestDto requestDto);

    void deleteReview(UUID reviewId, UUID currentUserId, boolean isModerator);

    Page<ReviewResponseDto> getTourReviews(UUID tourId, Pageable pageable);

    Page<ReviewResponseDto> getOrganizerReviews(UUID organizerId, Pageable pageable);

    Page<ReviewResponseDto> getMyReviews(UUID authorId, Pageable pageable);
}
