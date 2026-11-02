package com.tourhive.backend.auth;

import com.tourhive.backend.configurations.security.JwtTokenProvider;
import com.tourhive.backend.dto.auth.request.ForgotPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.LoginRequestDto;
import com.tourhive.backend.dto.auth.request.RegisterRequestDto;
import com.tourhive.backend.dto.auth.request.ResetPasswordRequestDto;
import com.tourhive.backend.dto.auth.request.VerifyEmailRequestDto;
import com.tourhive.backend.dto.auth.response.AuthResponseDto;
import com.tourhive.backend.entities.auth.EmailVerification;
import com.tourhive.backend.entities.auth.PasswordResetToken;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.mappers.users.UserMapper;
import com.tourhive.backend.mappers.users.UserMapperImpl;
import com.tourhive.backend.repositories.auth.EmailVerificationRepository;
import com.tourhive.backend.repositories.auth.PasswordResetTokenRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.auth.TokenService;
import com.tourhive.backend.services.auth.impl.AuthServiceImpl;
import com.tourhive.backend.services.email.EmailService;
import com.tourhive.backend.services.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    private final UserMapper userMapper = new UserMapperImpl();

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userService,
                tokenService,
                emailService,
                jwtTokenProvider,
                passwordEncoder,
                userMapper,
                emailVerificationRepository,
                passwordResetTokenRepository,
                userProfileRepository
        );
    }

    @Test
    @DisplayName("Should pass name and surname to createPendingUser during registration")
    void registerWithProfileInfo() {
        RegisterRequestDto request = new RegisterRequestDto("traveler@example.com", "Password123!", "John", "Doe");
        User user = User.builder().email("traveler@example.com").build();
        user.setId(UUID.randomUUID());

        when(userService.createPendingUser("traveler@example.com", "Password123!", "John", "Doe")).thenReturn(user);

        authService.register(request);

        verify(userService).createPendingUser("traveler@example.com", "Password123!", "John", "Doe");
        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(emailService).sendVerificationCode(eq("traveler@example.com"), anyString());
    }

    @Test
    @DisplayName("Should return AuthResponseDto with user summary upon email verification")
    void verifyEmailWithUserSummary() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("traveler@example.com").role(Role.USER).status(UserStatus.ACTIVE).build();
        user.setId(userId);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("John");
        profile.setLastName("Doe");

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .code("123456")
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(userService.getByEmail("traveler@example.com")).thenReturn(user);
        when(emailVerificationRepository.findTopByUserIdAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(userId, "123456"))
                .thenReturn(Optional.of(verification));
        when(userService.activateUser(user)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token-123");
        when(tokenService.issueRefreshToken(user)).thenReturn("refresh-token-123");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        AuthResponseDto response = authService.verifyEmail(new VerifyEmailRequestDto("traveler@example.com", "123456"));

        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-123");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().name()).isEqualTo("John");
        assertThat(response.user().surname()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should generate 6-digit OTP code on forgotPassword")
    void forgotPasswordGenerates6DigitCode() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("traveler@example.com").build();
        user.setId(userId);

        when(userService.getByEmail("traveler@example.com")).thenReturn(user);

        authService.forgotPassword(new ForgotPasswordRequestDto("traveler@example.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        String code = tokenCaptor.getValue().getToken();
        assertThat(code).hasSize(6);
        assertThat(code).containsOnlyDigits();
        verify(emailService).sendPasswordResetToken("traveler@example.com", code);
    }
}
