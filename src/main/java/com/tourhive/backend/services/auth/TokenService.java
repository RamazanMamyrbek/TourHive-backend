package com.tourhive.backend.services.auth;

import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.entities.users.User;

import java.util.UUID;

public interface TokenService {

    String issueRefreshToken(User user);

    AuthResponseDto rotateRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken);

    void revokeAllUserTokens(UUID userId);
}
