package com.tourhive.backend.auth;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-testing";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationSeconds", 900L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid JWT access token and extract correct claims")
    void generateTokenAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("traveler@tourhive.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("traveler@tourhive.com");
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should fail validation on invalid or tampered JWT")
    void validateTamperedToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }
}
