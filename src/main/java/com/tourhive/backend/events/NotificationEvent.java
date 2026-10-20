package com.tourhive.backend.events;

import com.tourhive.backend.entities.notifications.NotificationType;

import java.util.UUID;

public record NotificationEvent(
        UUID recipientUserId,
        NotificationType type,
        String title,
        String message,
        String entityType,
        UUID entityId
) {
}
