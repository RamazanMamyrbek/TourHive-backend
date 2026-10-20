package com.tourhive.backend.dto.notifications.response;

import com.tourhive.backend.entities.notifications.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Notification details response")
public record NotificationResponseDto(
        @Schema(description = "Notification ID")
        UUID id,

        @Schema(description = "Notification type")
        NotificationType type,

        @Schema(description = "Notification title")
        String title,

        @Schema(description = "Notification body text")
        String message,

        @Schema(description = "Target entity type (TOUR, APPLICATION, etc.)")
        String entityType,

        @Schema(description = "Target entity ID")
        UUID entityId,

        @Schema(description = "Read status")
        boolean isRead,

        @Schema(description = "Timestamp when marked as read")
        Instant readAt,

        @Schema(description = "Creation timestamp")
        Instant createdAt
) {
}
