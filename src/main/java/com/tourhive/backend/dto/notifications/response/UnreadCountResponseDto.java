package com.tourhive.backend.dto.notifications.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notifications count")
public record UnreadCountResponseDto(
        @Schema(description = "Count of unread notifications", example = "3")
        long unreadCount
) {
}
