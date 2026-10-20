package com.tourhive.backend.dto.reviews.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a tour review")
public record CreateReviewRequestDto(
        @Schema(description = "Rating from 1 to 5", example = "5")
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating,

        @Schema(description = "Optional review text/comment", example = "Breathtaking experience! Outstanding organization.")
        @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
        String comment
) {
}
