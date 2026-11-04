package com.tourhive.backend.dto.users.response;

public record FollowStatusResponseDto(
        boolean isFollowing,
        boolean isFollowedBy,
        long followersCount,
        long followingCount
) {}

