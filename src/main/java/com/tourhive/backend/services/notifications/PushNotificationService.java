package com.tourhive.backend.services.notifications;

import java.util.UUID;

public interface PushNotificationService {

    void sendPush(UUID userId, String title, String message, String entityType, UUID entityId);
}
