package com.tourhive.backend.dto.tours.request;

import jakarta.validation.constraints.Size;

public record CancelTourRequestDto(
        @Size(max = 1000, message = "Cancellation reason cannot exceed 1000 characters")
        String reason
) {
    public CancelTourRequestDto {
        if (reason == null || reason.isBlank()) {
            reason = "Отменено организатором";
        }
    }
}
