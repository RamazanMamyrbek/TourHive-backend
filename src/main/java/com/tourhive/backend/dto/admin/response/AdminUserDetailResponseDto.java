package com.tourhive.backend.dto.admin.response;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed admin view of user with statistics")
public record AdminUserDetailResponseDto(
        UUID id,
        String email,
        String nickname,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        String country,
        String city,
        String phoneNumber,
        Role role,
        UserStatus status,
        boolean isOrganizer,
        boolean isBusinessVerified,
        String blockReason,
        long totalToursCreated,
        long totalApplicationsSubmitted,
        long totalReportsSubmitted,
        long totalReportsReceived,
        Instant createdAt
) {
}
