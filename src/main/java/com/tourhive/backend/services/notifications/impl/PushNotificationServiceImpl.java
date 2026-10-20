package com.tourhive.backend.services.notifications.impl;

import com.tourhive.backend.entities.notifications.DeviceToken;
import com.tourhive.backend.repositories.notifications.DeviceTokenRepository;
import com.tourhive.backend.services.notifications.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendPush(UUID userId, String title, String message, String entityType, UUID entityId) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens registered for user: {}", userId);
        } else {
            log.info("Push notification queued for {} device token(s) of user {}: title='{}'", tokens.size(), userId, title);
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    Map.of(
                            "title", title,
                            "message", message,
                            "entityType", entityType != null ? entityType : "",
                            "entityId", entityId != null ? entityId.toString() : ""
                    )
            );
        } catch (Exception ex) {
            log.debug("Could not dispatch WebSocket STOMP user notification: {}", ex.getMessage());
        }
    }
}
