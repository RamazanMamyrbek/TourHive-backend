package com.tourhive.backend.dto.admin.request;

import com.tourhive.backend.entities.tours.TourStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewTourRequestDto(
        @NotNull(message = "Decision status is required (PUBLISHED or REJECTED)")
        TourStatus status,

        String rejectionReason
) {
}
