package com.tourhive.backend.services.notifications;

import com.tourhive.backend.dto.notifications.request.RegisterDeviceTokenRequestDto;
import com.tourhive.backend.dto.notifications.response.NotificationResponseDto;
import com.tourhive.backend.dto.notifications.response.UnreadCountResponseDto;
import com.tourhive.backend.events.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationResponseDto> getMyNotifications(UUID userId, boolean unreadOnly, Pageable pageable);

    UnreadCountResponseDto getUnreadCount(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    void registerDeviceToken(UUID userId, RegisterDeviceTokenRequestDto requestDto);

    void removeDeviceToken(UUID userId, String token);

    void handleNotificationEvent(NotificationEvent event);
}
