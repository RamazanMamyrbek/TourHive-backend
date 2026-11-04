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
        String telegram,
        String whatsapp,
        String instagram,
        String contactEmail,
        String specialization,
        Integer experienceYears,
        String website,
        boolean isOrganizer,
        boolean isBusinessVerified,
        long followersCount,
        long followingCount,
        Instant createdAt,
        Instant updatedAt
) {
}
