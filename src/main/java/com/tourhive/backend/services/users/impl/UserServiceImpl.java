package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.UserAlreadyExistsException;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.exceptions.CannotDeleteAccountWithActiveToursException;
import com.tourhive.backend.repositories.auth.RefreshTokenRepository;
import com.tourhive.backend.repositories.notifications.DeviceTokenRepository;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional
    public User createPendingUser(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email " + normalizedEmail + " already exists");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .isOrganizer(false)
                .isBusinessVerified(false)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User activateUser(User user) {
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePassword(User user, String newRawPassword) {
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }

        LocalDate today = LocalDate.now();
        List<Tour> organizerTours = tourRepository.findAllByOrganizerIdOrderByCreatedAtDesc(userId);
        for (Tour tour : organizerTours) {
            if (tour.getStatus() == TourStatus.PUBLISHED && !tour.getEndDate().isBefore(today)) {
                List<TourApplication> accepted = tourApplicationRepository.findAllByTourIdAndStatusIn(
                        tour.getId(),
                        List.of(TourApplicationStatus.ACCEPTED)
                );
                if (!accepted.isEmpty()) {
                    throw new CannotDeleteAccountWithActiveToursException(
                            "Cannot delete account while you have active published upcoming tours with accepted participants. Please cancel your tours first."
                    );
                }
            }
        }

        for (Tour tour : organizerTours) {
            if (tour.getStatus() == TourStatus.DRAFT || (tour.getStatus() == TourStatus.PUBLISHED && !tour.getEndDate().isBefore(today))) {
                tour.setStatus(TourStatus.CANCELLED);
                tour.setRejectionReason("Organizer deleted their account");
                tourRepository.save(tour);
            }
        }

        List<TourApplication> activeApps = tourApplicationRepository.findAllByApplicantIdAndStatusIn(
                userId,
                List.of(TourApplicationStatus.PENDING, TourApplicationStatus.ACCEPTED)
        );
        for (TourApplication app : activeApps) {
            if (app.getStatus() == TourApplicationStatus.ACCEPTED) {
                Tour tour = app.getTour();
                tour.setAvailablePlaces(tour.getAvailablePlaces() + 1);
                tourRepository.save(tour);
            }
            app.setStatus(TourApplicationStatus.CANCELLED_BY_TRAVELER);
            app.setRejectionReason("Applicant account was deleted");
            tourApplicationRepository.save(app);
        }

        deviceTokenRepository.deleteAllByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);

        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFirstName("Deleted");
            profile.setLastName("User");
            profile.setNickname("deleted_" + userId.toString().substring(0, 8));
            profile.setAvatarUrl(null);
            profile.setBio(null);
            profile.setPhone(null);
            userProfileRepository.save(profile);
        });

        user.setEmail("deleted_" + userId + "@deleted.tourhive.com");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.DELETED);
        user.setOrganizer(false);
        user.setBusinessVerified(false);
        userRepository.save(user);
    }
}
