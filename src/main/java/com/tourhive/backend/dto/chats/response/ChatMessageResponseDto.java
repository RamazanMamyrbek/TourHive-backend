package com.tourhive.backend.dto.chats.response;

import com.tourhive.backend.entities.chats.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Chat message response")
public record ChatMessageResponseDto(
        @Schema(description = "Message ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID id,

        @Schema(description = "Chat ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID chatId,

        @Schema(description = "Sender user ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID senderId,

        @Schema(description = "Sender full name", example = "Алиса Смирнова")
        String senderName,

        @Schema(description = "Sender nickname", example = "alicesmith")
        String senderNickname,

        @Schema(description = "Sender avatar URL", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String senderAvatarUrl,

        @Schema(description = "Message type", example = "TEXT")
        MessageType messageType,

        @Schema(description = "Text content of the message", example = "Привет! Завтра сбор в 09:00.")
        String content,

        @Schema(description = "Attachment URL", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String attachmentUrl,

        @Schema(description = "Whether the current user sent this message", example = "true")
        boolean isMine,

        @Schema(description = "Number of members who have read this message", example = "3")
        int readCount,

        @Schema(description = "Whether this message has been read by the current user (or recipient in direct chat)", example = "true")
        boolean isRead,

        @Schema(description = "Message creation timestamp", example = "2026-10-20T10:15:30Z")
        Instant createdAt
) {}
