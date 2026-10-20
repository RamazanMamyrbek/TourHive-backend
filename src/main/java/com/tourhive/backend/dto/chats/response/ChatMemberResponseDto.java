package com.tourhive.backend.dto.chats.response;

import com.tourhive.backend.entities.chats.ChatMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Chat member details")
public record ChatMemberResponseDto(
        @Schema(description = "Membership record ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID id,

        @Schema(description = "User ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID userId,

        @Schema(description = "Full name of the member", example = "Иван Иванов")
        String fullName,

        @Schema(description = "Nickname of the member", example = "ivan_tourist")
        String nickname,

        @Schema(description = "Avatar URL of the member", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String avatarUrl,

        @Schema(description = "Role in chat (ORGANIZER, MEMBER)", example = "MEMBER")
        ChatMemberRole role,

        @Schema(description = "Whether membership is currently active", example = "true")
        boolean isActive,

        @Schema(description = "Timestamp when the user joined the chat", example = "2026-10-20T10:00:00Z")
        Instant joinedAt,

        @Schema(description = "Timestamp when user left the chat (or null if active)", example = "2026-10-20T12:00:00Z")
        Instant leftAt
) {}
