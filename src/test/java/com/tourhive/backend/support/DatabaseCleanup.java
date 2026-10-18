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
public class DatabaseCleanup {

    private final BusinessDocumentRepository businessDocumentRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final com.tourhive.backend.repositories.tours.TourImageRepository tourImageRepository;
    private final com.tourhive.backend.repositories.tours.TourRouteItemRepository tourRouteItemRepository;
    private final com.tourhive.backend.repositories.tours.TourRepository tourRepository;
    private final com.tourhive.backend.repositories.resources.ResourceRepository resourceRepository;
    private final UserBlockRepository userBlockRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;

    public DatabaseCleanup(
            BusinessDocumentRepository businessDocumentRepository,
            BusinessProfileRepository businessProfileRepository,
            OrganizerProfileRepository organizerProfileRepository,
            UserProfileRepository userProfileRepository,
            com.tourhive.backend.repositories.tours.TourImageRepository tourImageRepository,
            com.tourhive.backend.repositories.tours.TourRouteItemRepository tourRouteItemRepository,
            com.tourhive.backend.repositories.tours.TourRepository tourRepository,
            com.tourhive.backend.repositories.resources.ResourceRepository resourceRepository,
            UserBlockRepository userBlockRepository,
            EmailVerificationRepository emailVerificationRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository
    ) {
        this.businessDocumentRepository = businessDocumentRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.organizerProfileRepository = organizerProfileRepository;
        this.userProfileRepository = userProfileRepository;
        this.tourImageRepository = tourImageRepository;
        this.tourRouteItemRepository = tourRouteItemRepository;
        this.tourRepository = tourRepository;
        this.resourceRepository = resourceRepository;
        this.userBlockRepository = userBlockRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
    }

    public void clean() {
        tourImageRepository.deleteAll();
        tourRouteItemRepository.deleteAll();
        tourRepository.deleteAll();
        businessDocumentRepository.deleteAll();
        businessProfileRepository.deleteAll();
        organizerProfileRepository.deleteAll();
        userProfileRepository.deleteAll();
        resourceRepository.deleteAll();
        userBlockRepository.deleteAll();
        emailVerificationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
}
