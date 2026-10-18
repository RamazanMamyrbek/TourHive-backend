package com.tourhive.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourhive.backend.dto.auth.request.LoginRequestDto;
import com.tourhive.backend.dto.auth.request.RefreshTokenRequestDto;
import com.tourhive.backend.dto.auth.request.RegisterRequestDto;
import com.tourhive.backend.dto.auth.request.VerifyEmailRequestDto;
import com.tourhive.backend.entities.auth.EmailVerification;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.repositories.auth.EmailVerificationRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("Complete authentication lifecycle: register -> verify -> login -> me -> refresh -> reuse detection -> logout")
    void completeAuthenticationLifecycle() throws Exception {
        String email = "alice@tourhive.com";
        String password = "Password123!";

        // 1. Register new user
        RegisterRequestDto registerRequest = new RegisterRequestDto(email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(email);

        // 2. Attempt login before verification (should be forbidden 403)
        LoginRequestDto unverifiedLogin = new LoginRequestDto(email, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unverifiedLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_VERIFIED"));

        // 3. Verify email with generated code
        EmailVerification verification = emailVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow();

        VerifyEmailRequestDto verifyRequest = new VerifyEmailRequestDto(email, verification.getCode());
        MvcResult verifyResult = mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        // 4. Login with correct password
        LoginRequestDto loginRequest = new LoginRequestDto(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asText();
        String refreshToken = loginJson.get("data").get("refreshToken").asText();

        // 5. Access GET /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        // 6. Access GET /api/v1/auth/me without token (should be 401)
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 7. Refresh token
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto(refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("data").get("refreshToken").asText();

        // 8. Attempt reuse of old refresh token (Reuse Detection -> 401)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        // 9. Logout with new refresh token
        RefreshTokenRequestDto logoutRequest = new RefreshTokenRequestDto(newRefreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Registration with duplicate email should return 409 CONFLICT")
    void duplicateRegistrationShouldFail() throws Exception {
        String email = "bob@tourhive.com";
        RegisterRequestDto request = new RegisterRequestDto(email, "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }
}
