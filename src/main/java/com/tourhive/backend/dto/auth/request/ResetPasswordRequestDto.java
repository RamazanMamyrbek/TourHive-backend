package com.tourhive.backend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        String token,
        String email,
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
) {
    public ResetPasswordRequestDto(String token, String newPassword) {
        this(token, null, null, newPassword);
    }
}
