package com.tourhive.backend.dto.chats.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create or retrieve a direct 1-on-1 chat")
public record CreateDirectChatRequestDto(
        @Schema(description = "Recipient user ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        @NotNull(message = "Recipient user ID is required")
        UUID recipientId
) {}
