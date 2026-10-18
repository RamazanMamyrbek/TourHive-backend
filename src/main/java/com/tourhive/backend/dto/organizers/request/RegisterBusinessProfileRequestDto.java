package com.tourhive.backend.dto.organizers.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterBusinessProfileRequestDto(
        @NotBlank(message = "Legal name is required")
        @Size(max = 255, message = "Legal name must not exceed 255 characters")
        String legalName,

        @NotBlank(message = "Registration/Tax number is required")
        @Size(max = 100, message = "Registration number must not exceed 100 characters")
        String registrationNumber,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be valid")
        String contactEmail,

        @NotBlank(message = "Contact phone is required")
        @Size(max = 50, message = "Contact phone must not exceed 50 characters")
        String contactPhone,

        String description,

        @NotEmpty(message = "At least one verification document is required")
        List<@Valid BusinessDocumentRequestDto> documents
) {
}
