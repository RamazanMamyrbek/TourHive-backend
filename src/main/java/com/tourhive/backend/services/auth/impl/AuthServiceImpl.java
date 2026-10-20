package com.tourhive.backend.services.auth.impl;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.auth.request.ForgotPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.LoginRequestDto;
import com.tourhive.backend.dto.auth.request.RefreshTokenRequestDto;
import com.tourhive.backend.dto.auth.request.RegisterRequestDto;
import com.tourhive.backend.dto.auth.request.ResetPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.VerifyEmailRequestDto;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.dto.auth.response.UserSummaryResponseDto;
import com.tourhive.backend.entities.auth.EmailVerification;
import com.tourhive.backend.entities.auth.PasswordResetToken;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.AccountBlockedException;
import com.tourhive.backend.exceptions.AccountNotVerifiedException;
import com.tourhive.backend.exceptions.InvalidCredentialsException;
import com.tourhive.backend.exceptions.InvalidTokenException;
import com.tourhive.backend.exceptions.InvalidVerificationCodeException;
import com.tourhive.backend.mappers.users.UserMapper;
import com.tourhive.backend.repositories.auth.EmailVerificationRepository;
import com.tourhive.backend.repositories.auth.PasswordResetTokenRepository;
import com.tourhive.backend.services.auth.AuthService;
import com.tourhive.backend.services.auth.TokenService;
import com.tourhive.backend.services.email.EmailService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void register(RegisterRequestDto request) {
        log.info("Processing registration for email: {}", request.email());
        User user = userService.createPendingUser(request.email(), request.password());

        String verificationCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .code(verificationCode)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        emailVerificationRepository.save(verification);

        emailService.sendVerificationCode(user.getEmail(), verificationCode);
    }

    @Override
    @Transactional
    public AuthResponseDto verifyEmail(VerifyEmailRequestDto request) {
        log.info("Processing email verification for email: {}", request.email());
        User user = userService.getByEmail(request.email());

        EmailVerification verification = emailVerificationRepository
                .findTopByUserIdAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), request.code())
                .orElseThrow(() -> new InvalidVerificationCodeException("Invalid or expired verification code"));

        if (!verification.isValid()) {
            throw new InvalidVerificationCodeException("Verification code has expired");
        }

        verification.setUsedAt(Instant.now());
        emailVerificationRepository.save(verification);

        User activatedUser = userService.activateUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(activatedUser);
        String refreshToken = tokenService.issueRefreshToken(activatedUser);

        return AuthResponseDto.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Processing login for email: {}", request.email());
        User user;
        try {
            user = userService.getByEmail(request.email());
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new AccountNotVerifiedException("Account email is not verified. Please verify your email first.");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Account is blocked. Please contact support.");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new com.tourhive.backend.exceptions.AccountDeletedException("This account has been deleted.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = tokenService.issueRefreshToken(user);

        return AuthResponseDto.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {
        return tokenService.rotateRefreshToken(request.refreshToken());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        tokenService.revokeRefreshToken(request.refreshToken());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        log.info("Processing forgot password for email: {}", request.email());
        try {
            User user = userService.getByEmail(request.email());
            String resetToken = UUID.randomUUID().toString();

            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(resetToken)
                    .expiresAt(Instant.now().plusSeconds(1800))
                    .build();
            passwordResetTokenRepository.save(passwordResetToken);

            emailService.sendPasswordResetToken(user.getEmail(), resetToken);
        } catch (Exception e) {
            log.warn("Forgot password requested for non-existing email: {}", request.email());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        log.info("Processing reset password with token");
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedAtIsNull(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        User user = resetToken.getUser();
        userService.updatePassword(user, request.newPassword());
        tokenService.revokeAllUserTokens(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponseDto getCurrentUser(UserPrincipal principal) {
        User user = userService.getById(principal.getId());
        return userMapper.toSummaryDto(user);
    }
}
