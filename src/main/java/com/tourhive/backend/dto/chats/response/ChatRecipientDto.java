package com.tourhive.backend.dto.chats.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Information about the other participant in a direct chat")
public record ChatRecipientDto(
        @Schema(description = "Recipient user ID", example = "d3b07384-d113-494b-9c8e-aa67258e7278")
        UUID id,

        @Schema(description = "Full name of the user", example = "Алиса Смирнова")
        String fullName,

        @Schema(description = "Unique nickname of the user", example = "alicesmith")
        String nickname,

        @Schema(description = "Avatar URL of the user", example = "/api/v1/resources/11111111-2222-3333-4444-555555555555")
        String avatarUrl
) {}
