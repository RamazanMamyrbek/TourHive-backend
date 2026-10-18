package com.tourhive.backend.controllers.auth;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.auth.request.ForgotPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.LoginRequestDto;
import com.tourhive.backend.dto.auth.request.RefreshTokenRequestDto;
import com.tourhive.backend.dto.auth.request.RegisterRequestDto;
import com.tourhive.backend.dto.auth.request.ResetPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.VerifyEmailRequestDto;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.dto.auth.response.UserSummaryResponseDto;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for registration, login, token rotation, and password recovery")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account", description = "Creates a pending account and sends a 6-digit verification code to the provided email")
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Verification code sent to email", null));
    }

    @Operation(summary = "Verify email address with 6-digit code", description = "Activates the user account and returns the initial pair of JWT access and refresh tokens")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto request) {
        AuthResponseDto response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponseDto.success("Email verified successfully", response));
    }

    @Operation(summary = "Log in with email and password", description = "Authenticates an active user and returns JWT access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
    }

    @Operation(summary = "Refresh expired access token", description = "Rotates refresh token and returns a new access/refresh token pair")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponseDto.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Log out user", description = "Revokes the supplied refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request password reset", description = "Generates a password reset token and sends instructions to email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponseDto.success("If the account exists, instructions have been sent", null));
    }

    @Operation(summary = "Reset password using token", description = "Resets the account password and revokes all active sessions")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponseDto.success("Password has been reset successfully", null));
    }

    @Operation(summary = "Get current authenticated user", description = "Returns summary information for the authenticated Bearer token", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserSummaryResponseDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryResponseDto summary = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponseDto.success(summary));
    }
}
