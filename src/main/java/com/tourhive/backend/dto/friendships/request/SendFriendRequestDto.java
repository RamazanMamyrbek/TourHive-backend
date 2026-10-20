package com.tourhive.backend.dto.friendships.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendFriendRequestDto(
        @NotNull(message = "Receiver ID is required")
        UUID receiverId,

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message
) {
}
