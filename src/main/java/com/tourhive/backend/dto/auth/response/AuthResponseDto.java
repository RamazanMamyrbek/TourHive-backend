package com.tourhive.backend.dto.auth.response;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static AuthResponseDto of(String accessToken, String refreshToken, long expiresIn) {
        return new AuthResponseDto(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
