package com.tourhive.backend.dto.chats.response;

import com.tourhive.backend.entities.chats.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Chat summary for user's chat list")
public record ChatResponseDto(
        @Schema(description = "Chat ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID id,

        @Schema(description = "Chat type (DIRECT, TOUR_GROUP)", example = "DIRECT")
        ChatType type,

        @Schema(description = "Display title of the chat", example = "Алиса Смирнова")
        String title,

        @Schema(description = "Display avatar URL of the chat", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String avatarUrl,

        @Schema(description = "Associated tour ID (for tour group chats)", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID tourId,

        @Schema(description = "Associated tour title", example = "Поход на Кольсайские озёра")
        String tourTitle,

        @Schema(description = "Last message in the chat")
        ChatMessageResponseDto lastMessage,

        @Schema(description = "Number of unread messages for the current user", example = "2")
        int unreadCount,

        @Schema(description = "Number of active members in the chat", example = "5")
        int membersCount,

        @Schema(description = "Other participant details (for direct chats)")
        ChatRecipientDto recipient,

        @Schema(description = "Timestamp of last activity in the chat", example = "2026-10-20T10:30:00Z")
        Instant updatedAt
) {}
