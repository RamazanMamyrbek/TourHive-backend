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

    private final com.tourhive.backend.repositories.reports.ReportRepository reportRepository;
    private final com.tourhive.backend.repositories.audit.AuditLogRepository auditLogRepository;
    private final com.tourhive.backend.repositories.reviews.ReviewRepository reviewRepository;
    private final com.tourhive.backend.repositories.notifications.NotificationRepository notificationRepository;
    private final com.tourhive.backend.repositories.notifications.DeviceTokenRepository deviceTokenRepository;
    private final com.tourhive.backend.repositories.chats.MessageReadRepository messageReadRepository;
    private final com.tourhive.backend.repositories.chats.MessageRepository messageRepository;
    private final com.tourhive.backend.repositories.chats.ChatMemberRepository chatMemberRepository;
    private final com.tourhive.backend.repositories.chats.ChatRepository chatRepository;
    private final com.tourhive.backend.repositories.favorites.FavoriteRepository favoriteRepository;
    private final com.tourhive.backend.repositories.friendships.FriendRequestRepository friendRequestRepository;
    private final com.tourhive.backend.repositories.friendships.FriendshipRepository friendshipRepository;
    private final BusinessDocumentRepository businessDocumentRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final com.tourhive.backend.repositories.tours.TourApplicationRepository tourApplicationRepository;
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
            com.tourhive.backend.repositories.reports.ReportRepository reportRepository,
            com.tourhive.backend.repositories.audit.AuditLogRepository auditLogRepository,
            com.tourhive.backend.repositories.reviews.ReviewRepository reviewRepository,
            com.tourhive.backend.repositories.notifications.NotificationRepository notificationRepository,
            com.tourhive.backend.repositories.notifications.DeviceTokenRepository deviceTokenRepository,
            com.tourhive.backend.repositories.chats.MessageReadRepository messageReadRepository,
            com.tourhive.backend.repositories.chats.MessageRepository messageRepository,
            com.tourhive.backend.repositories.chats.ChatMemberRepository chatMemberRepository,
            com.tourhive.backend.repositories.chats.ChatRepository chatRepository,
            com.tourhive.backend.repositories.favorites.FavoriteRepository favoriteRepository,
            com.tourhive.backend.repositories.friendships.FriendRequestRepository friendRequestRepository,
            com.tourhive.backend.repositories.friendships.FriendshipRepository friendshipRepository,
            BusinessDocumentRepository businessDocumentRepository,
            BusinessProfileRepository businessProfileRepository,
            OrganizerProfileRepository organizerProfileRepository,
            UserProfileRepository userProfileRepository,
            com.tourhive.backend.repositories.tours.ApplicationStatusHistoryRepository applicationStatusHistoryRepository,
            com.tourhive.backend.repositories.tours.TourApplicationRepository tourApplicationRepository,
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
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.messageReadRepository = messageReadRepository;
        this.messageRepository = messageRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.chatRepository = chatRepository;
        this.favoriteRepository = favoriteRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.businessDocumentRepository = businessDocumentRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.organizerProfileRepository = organizerProfileRepository;
        this.userProfileRepository = userProfileRepository;
        this.applicationStatusHistoryRepository = applicationStatusHistoryRepository;
        this.tourApplicationRepository = tourApplicationRepository;
        this.tourImageRepository = tourImageRepository;
        this.tourRouteItemRepository = tourRouteItemRepository;
        this.tourRepository = tourRepository;
        this.resourceRepository = resourceRepository;
        this.userBlockRepository = userBlockRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public void clean() {
        reportRepository.deleteAll();
        auditLogRepository.deleteAll();
        deviceTokenRepository.deleteAll();
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        messageReadRepository.deleteAll();
        messageRepository.deleteAll();
        chatMemberRepository.deleteAll();
        chatRepository.deleteAll();
        favoriteRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        applicationStatusHistoryRepository.deleteAll();
        tourApplicationRepository.deleteAll();
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
