package com.tourhive.backend.dto.auth.response;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponseDto(
        UUID id,
        String email,
        Role role,
        UserStatus status,
        boolean isOrganizer,
        boolean isBusinessVerified,
        Instant createdAt
) {
}
