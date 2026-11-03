package com.tourhive.backend.dto.users.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCertificateRequestDto(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "File URL is required")
        @Size(max = 500, message = "File URL must not exceed 500 characters")
        String fileUrl,

        @Size(max = 255, message = "File name must not exceed 255 characters")
        String fileName,

        @Size(max = 50, message = "File type must not exceed 50 characters")
        String fileType,

        @Size(max = 255, message = "Issuer must not exceed 255 characters")
        String issuer,

        @Size(max = 50, message = "Issue year must not exceed 50 characters")
        String issueYear
) {
}

