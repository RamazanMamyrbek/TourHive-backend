package com.tourhive.backend.dto.users.response;

import java.time.Instant;
import java.util.UUID;

public record UserFollowSummaryDto(
        UUID userId,
        String email,
        String nickname,
        String fullName,
        String avatarUrl,
        boolean isFollowing,
        Instant followedAt
) {}

