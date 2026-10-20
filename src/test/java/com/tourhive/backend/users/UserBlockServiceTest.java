package com.tourhive.backend.users;

import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserBlock;
import com.tourhive.backend.exceptions.CannotBlockSelfException;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.users.UserService;
import com.tourhive.backend.services.users.impl.UserBlockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private com.tourhive.backend.repositories.friendships.FriendshipRepository friendshipRepository;

    @Mock
    private com.tourhive.backend.repositories.friendships.FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private UserBlockServiceImpl userBlockService;

    private UUID userA;
    private UUID userB;
    private User blocker;
    private User blocked;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();

        blocker = User.builder().email("blocker@tourhive.com").build();
        blocker.setId(userA);

        blocked = User.builder().email("blocked@tourhive.com").build();
        blocked.setId(userB);
    }

    @Test
    @DisplayName("Should throw CannotBlockSelfException when user attempts to block themselves")
    void shouldThrowWhenBlockingSelf() {
        assertThatThrownBy(() -> userBlockService.blockUser(userA, userA))
                .isInstanceOf(CannotBlockSelfException.class)
                .hasMessageContaining("Users cannot block themselves");

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully block another user")
    void shouldBlockOtherUser() {
        when(userService.getById(userA)).thenReturn(blocker);
        when(userService.getById(userB)).thenReturn(blocked);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(userA, userB)).thenReturn(false);

        userBlockService.blockUser(userA, userB);

        verify(userBlockRepository).save(any(UserBlock.class));
        verify(friendshipRepository).deleteFriendshipBetween(any(), any());
        verify(friendRequestRepository).cancelPendingRequestsBetween(userA, userB);
    }

    @Test
    @DisplayName("Should be idempotent when blocking an already blocked user")
    void shouldBeIdempotentWhenAlreadyBlocked() {
        when(userService.getById(userA)).thenReturn(blocker);
        when(userService.getById(userB)).thenReturn(blocked);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(userA, userB)).thenReturn(true);

        userBlockService.blockUser(userA, userB);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete block record when unblocking")
    void shouldUnblockUser() {
        UserBlock block = UserBlock.builder().blocker(blocker).blocked(blocked).build();
        when(userBlockRepository.findByBlockerIdAndBlockedId(userA, userB)).thenReturn(Optional.of(block));

        userBlockService.unblockUser(userA, userB);

        verify(userBlockRepository).delete(block);
    }

    @Test
    @DisplayName("Should check mutual block status correctly")
    void shouldCheckIsBlockedBetween() {
        when(userBlockRepository.isBlockedBetween(userA, userB)).thenReturn(true);

        boolean isBlocked = userBlockService.isBlockedBetween(userA, userB);

        assertThat(isBlocked).isTrue();
        verify(userBlockRepository).isBlockedBetween(userA, userB);
    }
}
