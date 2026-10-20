package com.tourhive.backend.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.notifications.request.RegisterDeviceTokenRequestDto;
import com.tourhive.backend.entities.notifications.DeviceToken;
import com.tourhive.backend.entities.notifications.Notification;
import com.tourhive.backend.entities.notifications.NotificationType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.notifications.DeviceTokenRepository;
import com.tourhive.backend.repositories.notifications.NotificationRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private String userToken;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        user = userRepository.save(User.builder()
                .email("user@test.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .firstName("John")
                .lastName("Doe")
                .build());

        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    @Test
    @DisplayName("getMyNotifications: should return 401 when unauthorized")
    void getMyNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getMyNotifications: should return paginated list of notifications")
    void getMyNotifications_Success() throws Exception {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM)
                .title("Welcome")
                .message("Welcome to TourHive")
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.TOUR_REVIEWED)
                .title("New review")
                .message("You got a review")
                .isRead(true)
                .build());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("unreadOnly", "true")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Welcome")));
    }

    @Test
    @DisplayName("getUnreadCount: should return unread notifications count")
    void getUnreadCount_Success() throws Exception {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_RECEIVED)
                .title("Application")
                .message("Application received")
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM)
                .title("System update")
                .message("System message")
                .isRead(true)
                .build());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unreadCount", is(1)));
    }

    @Test
    @DisplayName("markAsRead: should mark a single notification as read")
    void markAsRead_Success() throws Exception {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_ACCEPTED)
                .title("Accepted")
                .message("Your application was accepted")
                .isRead(false)
                .build());

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notification.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertTrue(updated.isRead());
    }

    @Test
    @DisplayName("markAsRead: should return 404 when notification does not exist or belongs to another user")
    void markAsRead_NotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", UUID.randomUUID())
                        .header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("markAllAsRead: should mark all unread notifications as read")
    void markAllAsRead_Success() throws Exception {
        Notification n1 = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_RECEIVED)
                .title("App 1")
                .message("App 1 message")
                .isRead(false)
                .build());

        Notification n2 = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_ACCEPTED)
                .title("App 2")
                .message("App 2 message")
                .isRead(false)
                .build());

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertTrue(notificationRepository.findById(n1.getId()).orElseThrow().isRead());
        assertTrue(notificationRepository.findById(n2.getId()).orElseThrow().isRead());
    }

    @Test
    @DisplayName("registerDeviceToken: should register and update device tokens")
    void registerDeviceToken_Success() throws Exception {
        RegisterDeviceTokenRequestDto request = new RegisterDeviceTokenRequestDto("fcm_token_123", "ANDROID");

        mockMvc.perform(post("/api/v1/notifications/device-token")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertTrue(deviceTokenRepository.findByToken("fcm_token_123").isPresent());
    }

    @Test
    @DisplayName("removeDeviceToken: should remove specific token or all tokens")
    void removeDeviceToken_Success() throws Exception {
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("token_a")
                .deviceType("ANDROID")
                .build());

        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("token_b")
                .deviceType("IOS")
                .build());

        mockMvc.perform(delete("/api/v1/notifications/device-token")
                        .param("token", "token_a")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertFalse(deviceTokenRepository.findByToken("token_a").isPresent());
        assertTrue(deviceTokenRepository.findByToken("token_b").isPresent());

        // Remove all
        mockMvc.perform(delete("/api/v1/notifications/device-token")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertEquals(0, deviceTokenRepository.findAllByUserId(user.getId()).size());
    }
}
