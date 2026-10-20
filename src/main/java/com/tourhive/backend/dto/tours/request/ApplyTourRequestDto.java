package com.tourhive.backend.dto.tours.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ApplyTourRequestDto(
        @Schema(description = "Introductory message or motivation for the organizer", example = "Hi, I have hiking experience and would love to join your group!")
        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message,

        @Schema(description = "Optional direct contact info (phone/telegram/whatsapp)", example = "+77011234567")
        @Size(max = 255, message = "Contact info must not exceed 255 characters")
        String contactInfo
) {
}
