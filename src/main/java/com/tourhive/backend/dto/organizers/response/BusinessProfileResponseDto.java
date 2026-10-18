package com.tourhive.backend.dto.organizers.response;

import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessProfileResponseDto(
        UUID id,
        String legalName,
        String registrationNumber,
        String country,
        String contactEmail,
        String contactPhone,
        String description,
        BusinessVerificationStatus status,
        String rejectionReason,
        List<BusinessDocumentResponseDto> documents,
        Instant createdAt
) {
}
