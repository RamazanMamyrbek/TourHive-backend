package com.tourhive.backend.notifications;

import com.tourhive.backend.dto.notifications.request.RegisterDeviceTokenRequestDto;
import com.tourhive.backend.dto.notifications.response.NotificationResponseDto;
import com.tourhive.backend.dto.notifications.response.UnreadCountResponseDto;
import com.tourhive.backend.entities.notifications.DeviceToken;
import com.tourhive.backend.entities.notifications.Notification;
import com.tourhive.backend.entities.notifications.NotificationType;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.events.NotificationEvent;
import com.tourhive.backend.mappers.notifications.NotificationMapper;
import com.tourhive.backend.repositories.notifications.DeviceTokenRepository;
import com.tourhive.backend.repositories.notifications.NotificationRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.notifications.PushNotificationService;
import com.tourhive.backend.services.notifications.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Spy
    private NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("user@tourhive.com").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Get my notifications with unread filter")
    void getMyNotificationsUnreadOnly() {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_ACCEPTED)
                .title("Accepted")
                .message("Your application was accepted")
                .isRead(false)
                .build();

        when(notificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponseDto> page = notificationService.getMyNotifications(user.getId(), true, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Accepted");
    }

    @Test
    @DisplayName("Get unread notifications count")
    void getUnreadCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(4L);

        UnreadCountResponseDto result = notificationService.getUnreadCount(user.getId());

        assertThat(result.unreadCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Mark specific notification as read")
    void markAsRead() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM)
                .title("Welcome")
                .message("Welcome to TourHive")
                .isRead(false)
                .build();
        notification.setId(notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, user.getId());

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Mark all notifications as read")
    void markAllAsRead() {
        notificationService.markAllAsRead(user.getId());

        verify(notificationRepository).markAllAsRead(eq(user.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("Register device token creates or updates token")
    void registerDeviceToken() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByToken("token_123")).thenReturn(Optional.empty());

        notificationService.registerDeviceToken(user.getId(), new RegisterDeviceTokenRequestDto("token_123", "ANDROID"));

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    @DisplayName("Handle NotificationEvent creates notification and triggers push")
    void handleNotificationEvent() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Notification notification = Notification.builder().user(user).build();
        notification.setId(UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationEvent event = new NotificationEvent(
                user.getId(),
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "Friend Request",
                "Alice wants to be friends",
                "FRIEND_REQUEST",
                UUID.randomUUID()
        );

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(pushNotificationService).sendPush(user.getId(), "Friend Request", "Alice wants to be friends", "FRIEND_REQUEST", event.entityId());
    }
}
