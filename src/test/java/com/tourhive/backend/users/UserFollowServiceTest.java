package com.tourhive.backend.users;

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
import com.tourhive.backend.services.users.UserService;
import com.tourhive.backend.services.users.impl.UserFollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private UserFollowServiceImpl userFollowService;

    private UUID userA;
    private UUID userB;
    private User follower;
    private User following;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        follower = User.builder().email("a@test.com").build();
        follower.setId(userA);
        following = User.builder().email("b@test.com").build();
        following.setId(userB);
    }

    @Test
    @DisplayName("follow: successfully follows target user")
    void follow_Success() {
        when(userBlockService.isBlockedBetween(userA, userB)).thenReturn(false);
        when(userService.getById(userA)).thenReturn(follower);
        when(userService.getById(userB)).thenReturn(following);
        when(userFollowRepository.existsByFollowerIdAndFollowingId(userA, userB)).thenReturn(false, true);
        when(userFollowRepository.existsByFollowerIdAndFollowingId(userB, userA)).thenReturn(false);
        when(userFollowRepository.countByFollowingId(userB)).thenReturn(1L);
        when(userFollowRepository.countByFollowerId(userB)).thenReturn(0L);

        FollowStatusResponseDto result = userFollowService.follow(userA, userB);

        verify(userFollowRepository).save(any(UserFollow.class));
        assertThat(result.isFollowing()).isTrue();
        assertThat(result.followersCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("follow: throws when trying to follow self")
    void follow_Self_ThrowsException() {
        assertThatThrownBy(() -> userFollowService.follow(userA, userA))
                .isInstanceOf(CannotFollowSelfException.class);
        verify(userFollowRepository, never()).save(any());
    }

    @Test
    @DisplayName("follow: throws when users are blocked")
    void follow_Blocked_ThrowsException() {
        when(userBlockService.isBlockedBetween(userA, userB)).thenReturn(true);

        assertThatThrownBy(() -> userFollowService.follow(userA, userB))
                .isInstanceOf(UserBlockedException.class);
        verify(userFollowRepository, never()).save(any());
    }

    @Test
    @DisplayName("unfollow: successfully removes follow")
    void unfollow_Success() {
        UserFollow follow = new UserFollow(follower, following);
        when(userFollowRepository.findByFollowerIdAndFollowingId(userA, userB)).thenReturn(Optional.of(follow));
        when(userFollowRepository.existsByFollowerIdAndFollowingId(userA, userB)).thenReturn(false);
        when(userFollowRepository.existsByFollowerIdAndFollowingId(userB, userA)).thenReturn(false);
        when(userFollowRepository.countByFollowingId(userB)).thenReturn(0L);
        when(userFollowRepository.countByFollowerId(userB)).thenReturn(0L);

        FollowStatusResponseDto result = userFollowService.unfollow(userA, userB);

        verify(userFollowRepository).delete(follow);
        assertThat(result.isFollowing()).isFalse();
        assertThat(result.followersCount()).isZero();
    }

    @Test
    @DisplayName("getFollowers: returns mapped list of followers")
    void getFollowers_ReturnsPagedList() {
        UserFollow follow = new UserFollow(follower, following);
        follow.setCreatedAt(Instant.now());
        Page<UserFollow> page = new PageImpl<>(List.of(follow));
        when(userFollowRepository.findAllByFollowingId(eq(userB), any())).thenReturn(page);
        when(userProfileRepository.findByUserId(userA)).thenReturn(Optional.of(
                UserProfile.builder().user(follower).firstName("John").lastName("Doe").nickname("johndoe").build()
        ));
        when(userFollowRepository.existsByFollowerIdAndFollowingId(userA, userA)).thenReturn(false);

        Page<UserFollowSummaryDto> result = userFollowService.getFollowers(userB, userA, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        UserFollowSummaryDto summary = result.getContent().get(0);
        assertThat(summary.userId()).isEqualTo(userA);
        assertThat(summary.nickname()).isEqualTo("johndoe");
        assertThat(summary.fullName()).isEqualTo("John Doe");
    }
}
