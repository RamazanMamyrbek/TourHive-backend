package com.tourhive.backend.dto.notifications.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for registering mobile device token")
public record RegisterDeviceTokenRequestDto(
        @Schema(description = "FCM device registration token", example = "fcm_token_sample_12345")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "Device platform (ANDROID, IOS, WEB)", example = "ANDROID")
        String deviceType
) {
}
