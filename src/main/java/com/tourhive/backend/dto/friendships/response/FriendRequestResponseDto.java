package com.tourhive.backend.dto.friendships.response;

import com.tourhive.backend.entities.friendships.FriendRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponseDto(
        UUID id,
        UUID senderId,
        String senderEmail,
        String senderNickname,
        String senderAvatarUrl,
        UUID receiverId,
        String receiverEmail,
        String receiverNickname,
        String receiverAvatarUrl,
        FriendRequestStatus status,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
}
