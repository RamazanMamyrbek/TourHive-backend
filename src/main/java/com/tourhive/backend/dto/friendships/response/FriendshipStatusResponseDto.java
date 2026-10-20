package com.tourhive.backend.dto.friendships.response;

import com.tourhive.backend.entities.friendships.FriendshipStatus;

import java.util.UUID;

public record FriendshipStatusResponseDto(
        UUID targetUserId,
        FriendshipStatus status,
        UUID pendingRequestId
) {
}
