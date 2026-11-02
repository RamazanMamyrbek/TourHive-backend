package com.tourhive.backend.configurations.security;

import com.tourhive.backend.entities.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Environment environment;

    @Value("${jwt.secret:tourhive-super-secure-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    private SecretKey secretKey;

    public JwtTokenProvider() {
        this.environment = null;
    }

    public JwtTokenProvider(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            if (jwtSecret == null || jwtSecret.isBlank() ||
                    "tourhive-super-secure-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256".equals(jwtSecret) ||
                    jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("Production environment must configure a secure, non-default JWT_SECRET of at least 256 bits (32 bytes)");
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("status", user.getStatus().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
