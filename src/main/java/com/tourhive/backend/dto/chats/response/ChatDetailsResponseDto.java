package com.tourhive.backend.dto.chats.response;

import com.tourhive.backend.entities.chats.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed chat information including active and past members")
public record ChatDetailsResponseDto(
        @Schema(description = "Chat ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID id,

        @Schema(description = "Chat type (DIRECT, TOUR_GROUP)", example = "TOUR_GROUP")
        ChatType type,

        @Schema(description = "Display title of the chat", example = "Поход на Кольсайские озёра")
        String title,

        @Schema(description = "Display avatar URL of the chat", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String avatarUrl,

        @Schema(description = "Associated tour ID (if group tour chat)", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID tourId,

        @Schema(description = "Associated tour title", example = "Поход на Кольсайские озёра")
        String tourTitle,

        @Schema(description = "List of members in the chat")
        List<ChatMemberResponseDto> members,

        @Schema(description = "Chat creation timestamp", example = "2026-10-20T09:00:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-10-20T10:30:00Z")
        Instant updatedAt
) {}
