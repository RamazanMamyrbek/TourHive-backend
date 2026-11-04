package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.dto.users.response.FollowStatusResponseDto;
import com.tourhive.backend.dto.users.response.UserFollowSummaryDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserFollow;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CannotFollowSelfException;
import com.tourhive.backend.exceptions.UserBlockedException;
import com.tourhive.backend.repositories.users.UserFollowRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.users.UserBlockService;
import com.tourhive.backend.services.users.UserFollowService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final UserBlockService userBlockService;

    @Override
    @Transactional
    public FollowStatusResponseDto follow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new CannotFollowSelfException("Cannot follow yourself");
        }
        if (userBlockService.isBlockedBetween(currentUserId, targetUserId)) {
            throw new UserBlockedException("Cannot follow blocked user");
        }

        User follower = userService.getById(currentUserId);
        User following = userService.getById(targetUserId);

        if (!userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            UserFollow follow = new UserFollow(follower, following);
            userFollowRepository.save(follow);
            log.info("User {} followed user {}", currentUserId, targetUserId);
        }

        return getFollowStatus(currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public FollowStatusResponseDto unfollow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new CannotFollowSelfException("Cannot unfollow yourself");
        }

        userFollowRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
                .ifPresent(follow -> {
                    userFollowRepository.delete(follow);
                    log.info("User {} unfollowed user {}", currentUserId, targetUserId);
                });

        return getFollowStatus(currentUserId, targetUserId);
    }

    @Override
    public FollowStatusResponseDto getFollowStatus(UUID currentUserId, UUID targetUserId) {
        boolean isFollowing = currentUserId != null && userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean isFollowedBy = currentUserId != null && userFollowRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId);
        long followersCount = userFollowRepository.countByFollowingId(targetUserId);
        long followingCount = userFollowRepository.countByFollowerId(targetUserId);

        return new FollowStatusResponseDto(isFollowing, isFollowedBy, followersCount, followingCount);
    }

    @Override
    public Page<UserFollowSummaryDto> getFollowers(UUID targetUserId, UUID currentUserId, Pageable pageable) {
        return userFollowRepository.findAllByFollowingId(targetUserId, pageable)
                .map(follow -> toUserFollowSummary(follow.getFollower(), follow.getCreatedAt(), currentUserId));
    }

    @Override
    public Page<UserFollowSummaryDto> getFollowing(UUID targetUserId, UUID currentUserId, Pageable pageable) {
        return userFollowRepository.findAllByFollowerId(targetUserId, pageable)
                .map(follow -> toUserFollowSummary(follow.getFollowing(), follow.getCreatedAt(), currentUserId));
    }

    private UserFollowSummaryDto toUserFollowSummary(User user, Instant followedAt, UUID currentUserId) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        String fullName = null;
        String nickname = null;
        String avatarUrl = null;

        if (profile != null) {
            nickname = profile.getNickname();
            avatarUrl = profile.getAvatarUrl();
            if (profile.getFirstName() != null || profile.getLastName() != null) {
                fullName = ((profile.getFirstName() != null ? profile.getFirstName() : "") + " " +
                        (profile.getLastName() != null ? profile.getLastName() : "")).trim();
                if (fullName.isEmpty()) {
                    fullName = null;
                }
            }
        }

        boolean isFollowing = currentUserId != null && userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());

        return new UserFollowSummaryDto(
                user.getId(),
                user.getEmail(),
                nickname,
                fullName,
                avatarUrl,
                isFollowing,
                followedAt
        );
    }
}

