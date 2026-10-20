package com.tourhive.backend.dto.chats.request;

import com.tourhive.backend.entities.chats.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to send a message to a chat")
public record SendMessageRequestDto(
        @Schema(description = "Text content of the message", example = "Привет! Во сколько завтра встреча?")
        @Size(max = 4000, message = "Message content must not exceed 4000 characters")
        String content,

        @Schema(description = "Type of message (TEXT, IMAGE, SYSTEM)", example = "TEXT")
        MessageType messageType,

        @Schema(description = "Attachment URL (e.g. uploaded photo or resource)", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        @Size(max = 500, message = "Attachment URL must not exceed 500 characters")
        String attachmentUrl
) {}
