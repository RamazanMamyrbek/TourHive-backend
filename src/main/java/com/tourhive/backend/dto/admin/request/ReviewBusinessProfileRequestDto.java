package com.tourhive.backend.dto.admin.request;

import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewBusinessProfileRequestDto(
        @NotNull(message = "Decision status is required")
        BusinessVerificationStatus status,

        String rejectionReason
) {
}
