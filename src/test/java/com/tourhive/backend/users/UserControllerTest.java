package com.tourhive.backend.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.users.request.UpdateAvatarRequestDto;
import com.tourhive.backend.dto.users.request.UpdateProfileRequestDto;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private com.tourhive.backend.support.DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.tourhive.backend.repositories.resources.ResourceRepository resourceRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.tourhive.backend.services.resources.ResourceService resourceService;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        testUser = userRepository.save(User.builder()
                .email("john@tourhive.com")
                .passwordHash("$2a$12$dummyhashforpasswordtesting123")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        token = jwtTokenProvider.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return initial empty profile for active user")
    void shouldReturnEmptyProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@tourhive.com"))
                .andExpect(jsonPath("$.data.isOrganizer").value(false))
                .andExpect(jsonPath("$.data.isBusinessVerified").value(false));

        assertThat(userProfileRepository.findByUserId(testUser.getId())).isPresent();
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me should update profile fields")
    void shouldUpdateProfile() throws Exception {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "John",
                "Doe",
                "johndoe",
                "Travel enthusiast and photographer",
                "Kazakhstan",
                "Almaty",
                "English, Russian, Kazakh",
                "+77015554433"
        );

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.nickname").value("johndoe"))
                .andExpect(jsonPath("$.data.city").value("Almaty"))
                .andExpect(jsonPath("$.data.country").value("Kazakhstan"))
                .andExpect(jsonPath("$.data.phone").value("+77015554433"));

        UserProfile saved = userProfileRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(saved.getNickname()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("POST /api/v1/users/me/avatar (json) should update avatar URL")
    void shouldUpdateAvatar() throws Exception {
        UpdateAvatarRequestDto request = new UpdateAvatarRequestDto("https://minio.local/tourhive-avatars/john.jpg");

        mockMvc.perform(post("/api/v1/users/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://minio.local/tourhive-avatars/john.jpg"));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/avatar (multipart) should upload image and update avatar URL")
    void shouldUploadAvatarMultipart() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "john-photo.jpg",
                "image/jpeg",
                "binary image data".getBytes()
        );

        com.tourhive.backend.entities.resources.Resource mockResource = resourceRepository.save(new com.tourhive.backend.entities.resources.Resource(
                "john-photo.jpg",
                "avatars/" + UUID.randomUUID() + ".jpg",
                "avatars",
                100L,
                com.tourhive.backend.entities.resources.ResourceType.IMAGE,
                "image/jpeg"
        ));
        UUID resourceId = mockResource.getId();

        org.mockito.Mockito.when(resourceService.saveResource(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("avatars")))
                .thenReturn(mockResource);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").value("/api/v1/resources/" + resourceId));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return public profile and hide email/phone")
    void shouldReturnPublicProfileWithoutPrivateData() throws Exception {
        userProfileRepository.save(UserProfile.builder()
                .user(testUser)
                .firstName("John")
                .lastName("Doe")
                .nickname("johndoe")
                .city("Almaty")
                .country("Kazakhstan")
                .phone("+77015554433")
                .build());

        mockMvc.perform(get("/api/v1/users/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.nickname").value("johndoe"))
                .andExpect(jsonPath("$.data.city").value("Almaty"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me should fail with 409 if nickname is already taken by another user")
    void shouldFailWhenNicknameIsTaken() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(otherUser)
                .nickname("johndoe")
                .build());

        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Another",
                "User",
                "johndoe",
                null, null, null, null, null
        );

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_TAKEN"));
    }
}
