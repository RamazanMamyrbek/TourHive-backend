package com.tourhive.backend.dto.reviews.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating an existing review")
public record UpdateReviewRequestDto(
        @Schema(description = "Updated rating from 1 to 5", example = "4")
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating,

        @Schema(description = "Updated review comment", example = "Great tour overall, minor delay at start.")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment
) {
}
