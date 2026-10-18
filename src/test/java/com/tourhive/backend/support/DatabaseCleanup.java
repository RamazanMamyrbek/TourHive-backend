package com.tourhive.backend.support;

import com.tourhive.backend.repositories.auth.EmailVerificationRepository;
import com.tourhive.backend.repositories.auth.PasswordResetTokenRepository;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.repositories.organizers.BusinessDocumentRepository;
import com.tourhive.backend.repositories.organizers.BusinessProfileRepository;
import com.tourhive.backend.repositories.organizers.OrganizerProfileRepository;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseCleanup {

    private final BusinessDocumentRepository businessDocumentRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserBlockRepository userBlockRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;

    public void clean() {
        businessDocumentRepository.deleteAll();
        businessProfileRepository.deleteAll();
        organizerProfileRepository.deleteAll();
        userProfileRepository.deleteAll();
        userBlockRepository.deleteAll();
        emailVerificationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
