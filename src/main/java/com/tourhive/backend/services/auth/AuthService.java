package com.tourhive.backend.services.auth;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.auth.request.ForgotPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.LoginRequestDto;
import com.tourhive.backend.dto.auth.request.RefreshTokenRequestDto;
import com.tourhive.backend.dto.auth.request.RegisterRequestDto;
import com.tourhive.backend.dto.auth.request.ResetPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.VerifyEmailRequestDto;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.dto.auth.response.UserSummaryResponseDto;

public interface AuthService {

    void register(RegisterRequestDto request);

    AuthResponseDto verifyEmail(VerifyEmailRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refreshToken(RefreshTokenRequestDto request);

    void logout(RefreshTokenRequestDto request);

    void forgotPassword(ForgotPasswordRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);

    UserSummaryResponseDto getCurrentUser(UserPrincipal principal);
}
