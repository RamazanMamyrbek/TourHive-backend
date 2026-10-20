package com.tourhive.backend.dto.tours.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CancelApplicationRequestDto(
        @Schema(description = "Reason for withdrawing application or cancelling participation", example = "Plans changed due to emergency")
        @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
        String reason
) {
}
