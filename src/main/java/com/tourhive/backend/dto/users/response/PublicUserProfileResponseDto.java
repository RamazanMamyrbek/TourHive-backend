package com.tourhive.backend.dto.users.response;

import java.util.UUID;

public record PublicUserProfileResponseDto(
        UUID userId,
        String firstName,
        String lastName,
        String nickname,
        String avatarUrl,
        String bio,
        String country,
        String city,
        String languages,
        boolean isOrganizer,
        boolean isBusinessVerified
) {
}
