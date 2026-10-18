package com.tourhive.backend.dto.admin.response;

import com.tourhive.backend.dto.organizers.response.BusinessDocumentResponseDto;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessProfileAdminResponseDto(
        UUID id,
        UUID userId,
        String userEmail,
        String legalName,
        String registrationNumber,
        String country,
        String contactEmail,
        String contactPhone,
        String description,
        BusinessVerificationStatus status,
        String rejectionReason,
        String reviewedByEmail,
        Instant reviewedAt,
        List<BusinessDocumentResponseDto> documents,
        Instant createdAt
) {
}
