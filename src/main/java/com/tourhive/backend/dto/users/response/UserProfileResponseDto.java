package com.tourhive.backend.dto.users.response;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponseDto(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String nickname,
        String avatarUrl,
        String bio,
        String country,
        String city,
        String languages,
        String phone,
        boolean isOrganizer,
        boolean isBusinessVerified,
        Instant createdAt,
        Instant updatedAt
) {
}
