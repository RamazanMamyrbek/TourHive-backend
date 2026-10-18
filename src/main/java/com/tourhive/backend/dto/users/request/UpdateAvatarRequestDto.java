package com.tourhive.backend.dto.users.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequestDto(
        @NotBlank(message = "Avatar URL is required")
        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl
) {
}
