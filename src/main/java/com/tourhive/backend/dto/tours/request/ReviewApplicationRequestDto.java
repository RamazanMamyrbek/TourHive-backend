package com.tourhive.backend.dto.tours.request;

import com.tourhive.backend.entities.tours.TourApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewApplicationRequestDto(
        @Schema(description = "New application status: ACCEPTED or REJECTED", example = "ACCEPTED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Status is required")
        TourApplicationStatus status,

        @Schema(description = "Optional rejection reason if status is REJECTED", example = "No spots left for requested dates")
        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String rejectionReason
) {
}
