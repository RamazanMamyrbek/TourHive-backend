package com.tourhive.backend.services.notifications.impl;

import com.tourhive.backend.dto.notifications.request.RegisterDeviceTokenRequestDto;
import com.tourhive.backend.dto.notifications.response.NotificationResponseDto;
import com.tourhive.backend.dto.notifications.response.UnreadCountResponseDto;
import com.tourhive.backend.entities.notifications.DeviceToken;
import com.tourhive.backend.entities.notifications.Notification;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.events.NotificationEvent;
import com.tourhive.backend.exceptions.NotificationNotFoundException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.mappers.notifications.NotificationMapper;
import com.tourhive.backend.repositories.notifications.DeviceTokenRepository;
import com.tourhive.backend.repositories.notifications.NotificationRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.notifications.NotificationService;
import com.tourhive.backend.services.notifications.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationService pushNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getMyNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return notifications.map(notificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponseDto getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponseDto(count);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationNotFoundException("Notification not found for user: " + notificationId);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    @Transactional
    public void registerDeviceToken(UUID userId, RegisterDeviceTokenRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String token = requestDto.token().trim();
        String deviceType = requestDto.deviceType() != null && !requestDto.deviceType().isBlank()
                ? requestDto.deviceType().trim().toUpperCase()
                : "ANDROID";

        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> {
                    existing.setUser(user);
                    existing.setDeviceType(deviceType);
                    deviceTokenRepository.save(existing);
                },
                () -> {
                    DeviceToken newToken = DeviceToken.builder()
                            .user(user)
                            .token(token)
                            .deviceType(deviceType)
                            .build();
                    deviceTokenRepository.save(newToken);
                }
        );
        log.info("Device token registered for user: {}", userId);
    }

    @Override
    @Transactional
    public void removeDeviceToken(UUID userId, String token) {
        if (token != null && !token.isBlank()) {
            deviceTokenRepository.deleteByTokenAndUserId(token.trim(), userId);
        } else {
            deviceTokenRepository.deleteAllByUserId(userId);
        }
        log.info("Device token(s) removed for user: {}", userId);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        User user = userRepository.findById(event.recipientUserId()).orElse(null);
        if (user == null) {
            log.warn("Cannot create notification, user not found: {}", event.recipientUserId());
            return;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(event.type())
                .title(event.title())
                .message(event.message())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [id={}, type={}] for user: {}", saved.getId(), event.type(), user.getId());

        pushNotificationService.sendPush(user.getId(), event.title(), event.message(), event.entityType(), event.entityId());
    }
}
