package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ApplicationHistoryItemResponseDto(
        @Schema(description = "History entry ID")
        UUID id,

        @Schema(description = "Previous application status")
        TourApplicationStatus oldStatus,

        @Schema(description = "New application status")
        TourApplicationStatus newStatus,

        @Schema(description = "User ID who performed the status transition")
        UUID changedById,

        @Schema(description = "Reason provided for the transition")
        String reason,

        @Schema(description = "Timestamp when transition occurred")
        Instant createdAt
) {
}
