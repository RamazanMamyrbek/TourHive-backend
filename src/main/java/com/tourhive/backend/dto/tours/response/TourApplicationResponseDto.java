package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TourApplicationResponseDto(
        @Schema(description = "Application ID")
        UUID id,

        @Schema(description = "Target tour ID")
        UUID tourId,

        @Schema(description = "Tour title")
        String tourTitle,

        @Schema(description = "Applicant user ID")
        UUID applicantId,

        @Schema(description = "Applicant nickname")
        String applicantNickname,

        @Schema(description = "Applicant full name")
        String applicantFullName,

        @Schema(description = "Applicant avatar URL")
        String applicantAvatarUrl,

        @Schema(description = "Current application status")
        TourApplicationStatus status,

        @Schema(description = "Applicant motivation message")
        String message,

        @Schema(description = "Applicant contact details")
        String contactInfo,

        @Schema(description = "Reason if rejected by organizer")
        String rejectionReason,

        @Schema(description = "Reason if cancelled or withdrawn")
        String cancellationReason,

        @Schema(description = "Submission timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt,

        @Schema(description = "History of status transitions")
        List<ApplicationHistoryItemResponseDto> history
) {
}
