package com.tourhive.backend.dto.users.response;

import java.time.Instant;
import java.util.UUID;

public record BlockedUserResponseDto(
        UUID id,
        UUID blockedUserId,
        String email,
        String nickname,
        String avatarUrl,
        Instant blockedAt
) {
}
