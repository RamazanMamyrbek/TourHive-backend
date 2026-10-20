package com.tourhive.backend.dto.admin.response;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Admin view of user summary")
public record AdminUserResponseDto(
        UUID id,
        String email,
        String nickname,
        String firstName,
        String lastName,
        Role role,
        UserStatus status,
        boolean isOrganizer,
        boolean isBusinessVerified,
        String blockReason,
        Instant createdAt
) {
}
