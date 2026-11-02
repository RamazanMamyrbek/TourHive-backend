package com.tourhive.backend.auth;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.entities.auth.RefreshToken;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.InvalidTokenException;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.services.auth.TokenService;
import com.tourhive.backend.services.auth.impl.TokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(refreshTokenRepository, jwtTokenProvider);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationSeconds", 2592000L);
    }

    @Test
    @DisplayName("Should detect replay of revoked refresh token and revoke all user tokens")
    void detectReplayAndRevokeAllSessions() {

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("test@tourhive.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);

        RefreshToken revokedToken = RefreshToken.builder()
                .user(user)
                .tokenHash("hash123")
                .expiresAt(Instant.now().plusSeconds(1000))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken("raw-token-123"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("All sessions revoked");

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    @DisplayName("Should rotate valid refresh token successfully")
    void rotateValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("test@tourhive.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);

        RefreshToken activeToken = RefreshToken.builder()
                .user(user)
                .tokenHash("hash123")
                .expiresAt(Instant.now().plusSeconds(1000))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(activeToken));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponseDto response = tokenService.rotateRefreshToken("raw-token-123");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
