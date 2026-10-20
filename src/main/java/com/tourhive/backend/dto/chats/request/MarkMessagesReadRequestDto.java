package com.tourhive.backend.dto.chats.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Request to mark messages as read up to specified message")
public record MarkMessagesReadRequestDto(
        @Schema(description = "Optional message ID up to which all messages should be marked as read. If null, marks all messages as read.", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID lastMessageId
) {}
