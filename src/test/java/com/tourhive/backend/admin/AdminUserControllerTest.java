package com.tourhive.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.dto.admin.request.BlockUserRequestDto;
import com.tourhive.backend.dto.admin.request.UpdateUserRoleRequestDto;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.configurations.security.JwtTokenProvider;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User regularUser;
    private User moderator;
    private User admin;
    private String userToken;
    private String moderatorToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        regularUser = userRepository.save(User.builder()
                .email("regular@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        moderator = userRepository.save(User.builder()
                .email("moderator@tourhive.com")
                .passwordHash("hash")
                .role(Role.MODERATOR)
                .status(UserStatus.ACTIVE)
                .build());

        admin = userRepository.save(User.builder()
                .email("admin@tourhive.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        userToken = jwtTokenProvider.generateAccessToken(regularUser);
        moderatorToken = jwtTokenProvider.generateAccessToken(moderator);
        adminToken = jwtTokenProvider.generateAccessToken(admin);
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - moderator can list users")
    void listUsers_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - regular user gets 403 Forbidden")
    void listUsers_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{userId} - view user details")
    void getUserDetail_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + regularUser.getId())
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(regularUser.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("regular@tourhive.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/users/{userId}/block & unblock - updates status and rejects blocked JWT")
    void blockAndUnblockUser_Success() throws Exception {
        BlockUserRequestDto blockDto = new BlockUserRequestDto("Repeated spam violations");

        // 1. Block user
        mockMvc.perform(post("/api/v1/admin/users/" + regularUser.getId() + "/block")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.blockReason").value("Repeated spam violations"));

        // 2. Blocked user tries to access protected endpoint -> 403
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));

        // 3. Unblock user
        mockMvc.perform(post("/api/v1/admin/users/" + regularUser.getId() + "/unblock")
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 4. User can access again
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/role - ADMIN only can update role")
    void updateUserRole_Success() throws Exception {
        UpdateUserRoleRequestDto roleDto = new UpdateUserRoleRequestDto(Role.MODERATOR);

        // Moderator attempt -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/admin/users/" + regularUser.getId() + "/role")
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isForbidden());

        // Admin attempt -> 200 OK
        mockMvc.perform(patch("/api/v1/admin/users/" + regularUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MODERATOR"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me - user self-deletion marks DELETED and rejects subsequent requests")
    void deleteMyAccount_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        User deletedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals(UserStatus.DELETED, deletedUser.getStatus());

        // Subsequent request with that user's token is rejected with 403 ACCOUNT_DELETED
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DELETED"));
    }
}
