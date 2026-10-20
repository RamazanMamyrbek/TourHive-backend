package com.tourhive.backend.dto.tours.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTourRequestDto(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 1000, message = "Cancellation reason cannot exceed 1000 characters")
        String reason
) {
}
