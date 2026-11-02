package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
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

import com.tourhive.backend.entities.chats.ChatMember;
import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import com.tourhive.backend.repositories.chats.ChatMemberRepository;
import com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TourRepository tourRepository;
    private final TourApplicationRepository tourApplicationRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final ChatMemberRepository chatMemberRepository;
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
        return createPendingUser(email, rawPassword, null, null);
    }

    @Override
    @Transactional
    public User createPendingUser(String email, String rawPassword, String name, String surname) {
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

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        if (name != null && !name.isBlank()) {
            profile.setFirstName(name.trim());
        }
        if (surname != null && !surname.isBlank()) {
            profile.setLastName(surname.trim());
        }
        userProfileRepository.save(profile);

        return savedUser;
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
                tour.setCancellationReason("Organizer deleted their account");
                tour.setAvailablePlaces(0);
                tourRepository.save(tour);

                List<TourApplication> tourApps = tourApplicationRepository.findAllByTourIdAndStatusIn(
                        tour.getId(),
                        List.of(TourApplicationStatus.PENDING, TourApplicationStatus.ACCEPTED)
                );
                for (TourApplication tourApp : tourApps) {
                    TourApplicationStatus oldStatus = tourApp.getStatus();
                    tourApp.setStatus(TourApplicationStatus.CANCELLED_BY_ORGANIZER);
                    tourApp.setCancellationReason("Organizer deleted their account");
                    tourApplicationRepository.save(tourApp);

                    ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                            .application(tourApp)
                            .oldStatus(oldStatus)
                            .newStatus(TourApplicationStatus.CANCELLED_BY_ORGANIZER)
                            .changedBy(user)
                            .reason("Organizer deleted their account")
                            .build();
                    applicationStatusHistoryRepository.save(history);
                }
            }
        }

        List<TourApplication> activeApps = tourApplicationRepository.findAllByApplicantIdAndStatusIn(
                userId,
                List.of(TourApplicationStatus.PENDING, TourApplicationStatus.ACCEPTED)
        );
        for (TourApplication app : activeApps) {
            Tour tour = app.getTour();
            if (!tour.getEndDate().isBefore(today)) {
                if (app.getStatus() == TourApplicationStatus.ACCEPTED) {
                    tour.setAvailablePlaces(Math.min(tour.getCapacity(), tour.getAvailablePlaces() + 1));
                    tourRepository.save(tour);
                }
                TourApplicationStatus oldStatus = app.getStatus();
                app.setStatus(TourApplicationStatus.CANCELLED_BY_TRAVELER);
                app.setCancellationReason("Applicant account was deleted");
                tourApplicationRepository.save(app);

                ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                        .application(app)
                        .oldStatus(oldStatus)
                        .newStatus(TourApplicationStatus.CANCELLED_BY_TRAVELER)
                        .changedBy(user)
                        .reason("Applicant account was deleted")
                        .build();
                applicationStatusHistoryRepository.save(history);
            }
        }

        deviceTokenRepository.deleteAllByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);

        chatMemberRepository.findAllByUserId(userId).forEach(member -> {
            member.setActive(false);
            member.setLeftAt(java.time.Instant.now());
            chatMemberRepository.save(member);
        });

        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFirstName("Deleted");
            profile.setLastName("User");
            profile.setNickname("deleted_" + userId.toString().substring(0, 8));
            profile.setAvatarUrl(null);
            profile.setAvatarResource(null);
            profile.setBio(null);
            profile.setPhone(null);
            profile.setCountry(null);
            profile.setCity(null);
            profile.setLanguages(null);
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
