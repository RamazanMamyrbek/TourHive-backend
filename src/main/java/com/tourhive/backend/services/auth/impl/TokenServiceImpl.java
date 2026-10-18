package com.tourhive.backend.services.auth.impl;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.entities.auth.RefreshToken;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.AccountBlockedException;
import com.tourhive.backend.exceptions.InvalidTokenException;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.services.auth.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token-expiration-seconds:2592000}")
    private long refreshTokenExpirationSeconds;

    @Override
    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    @Transactional
    public AuthResponseDto rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        User user = token.getUser();

        if (token.isRevoked()) {
            log.warn("Detected attempt to reuse revoked refresh token for user {}", user.getId());
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw new InvalidTokenException("Invalid or compromised refresh token. All sessions revoked.");
        }

        if (token.isExpired()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Account is blocked");
        }

        String newRawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String newTokenHash = hashToken(newRawRefreshToken);

        token.setRevoked(true);
        token.setReplacedByToken(newTokenHash);
        refreshTokenRepository.save(token);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirationSeconds))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponseDto.of(
                newAccessToken,
                newRawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
