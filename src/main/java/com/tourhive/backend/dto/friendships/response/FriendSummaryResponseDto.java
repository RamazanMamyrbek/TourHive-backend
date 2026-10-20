package com.tourhive.backend.dto.friendships.response;

import java.time.Instant;
import java.util.UUID;

public record FriendSummaryResponseDto(
        UUID userId,
        String email,
        String nickname,
        String firstName,
        String lastName,
        String avatarUrl,
        String city,
        String country,
        Instant friendshipSince
) {
}
