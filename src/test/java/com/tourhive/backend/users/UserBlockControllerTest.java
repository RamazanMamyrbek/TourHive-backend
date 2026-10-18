package com.tourhive.backend.users;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserBlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private com.tourhive.backend.support.DatabaseCleanup databaseCleanup;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        userA = userRepository.save(User.builder()
                .email("usera@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        userB = userRepository.save(User.builder()
                .email("userb@tourhive.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build());

        userProfileRepository.save(UserProfile.builder().user(userA).nickname("alice").build());
        userProfileRepository.save(UserProfile.builder().user(userB).nickname("bob").build());

        tokenA = jwtTokenProvider.generateAccessToken(userA);
        tokenB = jwtTokenProvider.generateAccessToken(userB);
    }

    @Test
    @DisplayName("Should block, list, and unblock user successfully")
    void shouldHandleBlockingLifecycle() throws Exception {
        // 1. Block user B
        mockMvc.perform(post("/api/v1/users/" + userB.getId() + "/block")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId())).isTrue();

        // 2. User B cannot view User A's public profile (hidden / 404)
        mockMvc.perform(get("/api/v1/users/" + userA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // 3. User A views list of blocked users
        mockMvc.perform(get("/api/v1/users/me/blocks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].blockedUserId").value(userB.getId().toString()))
                .andExpect(jsonPath("$.data[0].email").value("userb@tourhive.com"))
                .andExpect(jsonPath("$.data[0].nickname").value("bob"));

        // 4. User A unblocks user B
        mockMvc.perform(delete("/api/v1/users/" + userB.getId() + "/block")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(userA.getId(), userB.getId())).isFalse();

        // 5. Verify blocks list is empty
        mockMvc.perform(get("/api/v1/users/me/blocks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Should reject blocking oneself with 400 CANNOT_BLOCK_SELF")
    void shouldRejectSelfBlock() throws Exception {
        mockMvc.perform(post("/api/v1/users/" + userA.getId() + "/block")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_BLOCK_SELF"));
    }
}
