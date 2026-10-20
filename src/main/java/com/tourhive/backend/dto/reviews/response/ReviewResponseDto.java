package com.tourhive.backend.dto.reviews.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Review details and author information")
public record ReviewResponseDto(
        @Schema(description = "Review ID")
        UUID id,

        @Schema(description = "Tour ID")
        UUID tourId,

        @Schema(description = "Tour Title")
        String tourTitle,

        @Schema(description = "Organizer User ID")
        UUID organizerId,

        @Schema(description = "Organizer Name")
        String organizerName,

        @Schema(description = "Author User ID")
        UUID authorId,

        @Schema(description = "Author Full Name")
        String authorName,

        @Schema(description = "Author Nickname")
        String authorNickname,

        @Schema(description = "Author Avatar URL")
        String authorAvatarUrl,

        @Schema(description = "Rating (1 to 5)", example = "5")
        int rating,

        @Schema(description = "Review comment text")
        String comment,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
}
