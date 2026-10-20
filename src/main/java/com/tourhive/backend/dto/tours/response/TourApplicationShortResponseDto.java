package com.tourhive.backend.dto.tours.response;

import com.tourhive.backend.entities.tours.TourApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TourApplicationShortResponseDto(
        @Schema(description = "Application ID")
        UUID id,

        @Schema(description = "Target tour ID")
        UUID tourId,

        @Schema(description = "Tour title")
        String tourTitle,

        @Schema(description = "Tour cover image URL")
        String tourCoverImageUrl,

        @Schema(description = "Tour start date")
        LocalDate tourStartDate,

        @Schema(description = "Tour end date")
        LocalDate tourEndDate,

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

        @Schema(description = "Submission timestamp")
        Instant createdAt
) {
}
