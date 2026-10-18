package com.tourhive.backend.dto.organizers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessDocumentRequestDto(
        @NotBlank(message = "Document name is required")
        @Size(max = 255, message = "Document name must not exceed 255 characters")
        String documentName,

        @NotBlank(message = "Document URL is required")
        @Size(max = 500, message = "Document URL must not exceed 500 characters")
        String documentUrl
) {
}
