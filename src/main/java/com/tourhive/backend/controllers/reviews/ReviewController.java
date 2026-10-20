package com.tourhive.backend.controllers.reviews;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.reviews.request.CreateReviewRequestDto;
import com.tourhive.backend.dto.reviews.request.UpdateReviewRequestDto;
import com.tourhive.backend.dto.reviews.response.ReviewResponseDto;
import com.tourhive.backend.services.reviews.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reviews", description = "Endpoints for tour and organizer reviews and ratings")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Create review for tour",
            description = "Creates a review for completed tour by accepted participant",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/tours/{tourId}/reviews")
    public ResponseEntity<ApiResponseDto<ReviewResponseDto>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID tourId,
            @Valid @RequestBody CreateReviewRequestDto requestDto
    ) {
        ReviewResponseDto response = reviewService.createReview(tourId, principal.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Review submitted successfully", response));
    }

    @Operation(
            summary = "Get reviews for tour",
            description = "Returns paginated list of reviews for a tour (public)"
    )
    @GetMapping("/tours/{tourId}/reviews")
    public ResponseEntity<ApiResponseDto<Page<ReviewResponseDto>>> getTourReviews(
            @PathVariable UUID tourId,
            @ParameterObject Pageable pageable
    ) {
        Page<ReviewResponseDto> response = reviewService.getTourReviews(tourId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get reviews for organizer",
            description = "Returns paginated list of reviews for an organizer (public)"
    )
    @GetMapping("/organizers/{organizerId}/reviews")
    public ResponseEntity<ApiResponseDto<Page<ReviewResponseDto>>> getOrganizerReviews(
            @PathVariable UUID organizerId,
            @ParameterObject Pageable pageable
    ) {
        Page<ReviewResponseDto> response = reviewService.getOrganizerReviews(organizerId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Get my reviews",
            description = "Returns paginated list of reviews authored by current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reviews/my")
    public ResponseEntity<ApiResponseDto<Page<ReviewResponseDto>>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<ReviewResponseDto> response = reviewService.getMyReviews(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(
            summary = "Update review",
            description = "Updates rating and comment of a review by author",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/reviews/{id}")
    public ResponseEntity<ApiResponseDto<ReviewResponseDto>> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequestDto requestDto
    ) {
        ReviewResponseDto response = reviewService.updateReview(id, principal.getId(), requestDto);
        return ResponseEntity.ok(ApiResponseDto.success("Review updated successfully", response));
    }

    @Operation(
            summary = "Delete review",
            description = "Deletes a review by author or moderator/admin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        boolean isModerator = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR") || a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(id, principal.getId(), isModerator);
        return ResponseEntity.ok(ApiResponseDto.success("Review deleted successfully", null));
    }
}
