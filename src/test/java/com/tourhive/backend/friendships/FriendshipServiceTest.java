package com.tourhive.backend.friendships;

import com.tourhive.backend.dto.friendships.request.SendFriendRequestDto;
import com.tourhive.backend.dto.friendships.response.FriendRequestResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendSummaryResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendshipStatusResponseDto;
import com.tourhive.backend.entities.friendships.FriendRequest;
import com.tourhive.backend.entities.friendships.FriendRequestStatus;
import com.tourhive.backend.entities.friendships.Friendship;
import com.tourhive.backend.entities.friendships.FriendshipStatus;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.AlreadyFriendsException;
import com.tourhive.backend.exceptions.CannotBefriendSelfException;
import com.tourhive.backend.exceptions.DuplicateFriendRequestException;
import com.tourhive.backend.exceptions.FriendRequestAccessDeniedException;
import com.tourhive.backend.exceptions.IncomingFriendRequestAlreadyExistsException;
import com.tourhive.backend.exceptions.InvalidFriendRequestStateException;
import com.tourhive.backend.exceptions.UserBlockedException;
import com.tourhive.backend.mappers.friendships.FriendshipMapper;
import com.tourhive.backend.repositories.friendships.FriendRequestRepository;
import com.tourhive.backend.repositories.friendships.FriendshipRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.friendships.impl.FriendshipServiceImpl;
import com.tourhive.backend.services.users.UserBlockService;
import com.tourhive.backend.services.users.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserBlockService userBlockService;

    @Mock
    private FriendshipMapper friendshipMapper;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    private UUID userAId;
    private UUID userBId;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        userA = User.builder().email("userA@tourhive.com").build();
        userA.setId(userAId);

        userB = User.builder().email("userB@tourhive.com").build();
        userB.setId(userBId);
    }

    @Test
    @DisplayName("Should throw CannotBefriendSelfException when sending request to self")
    void shouldThrowWhenSendingRequestToSelf() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userAId, "Hello myself");

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(userAId, dto))
                .isInstanceOf(CannotBefriendSelfException.class)
                .hasMessageContaining("Cannot send friend request to yourself");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserBlockedException when communication is blocked")
    void shouldThrowWhenUsersAreBlocked() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userBId, "Hello");
        when(userService.getById(userAId)).thenReturn(userA);
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(userAId, dto))
                .isInstanceOf(UserBlockedException.class)
                .hasMessageContaining("communication is blocked");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AlreadyFriendsException when users are already friends")
    void shouldThrowWhenAlreadyFriends() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userBId, "Hello");
        when(userService.getById(userAId)).thenReturn(userA);
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(userAId, dto))
                .isInstanceOf(AlreadyFriendsException.class)
                .hasMessageContaining("already friends");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateFriendRequestException when outgoing request already pending")
    void shouldThrowWhenDuplicateOutgoingRequest() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userBId, "Hello");
        when(userService.getById(userAId)).thenReturn(userA);
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userAId, userBId, FriendRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(userAId, dto))
                .isInstanceOf(DuplicateFriendRequestException.class)
                .hasMessageContaining("Pending friend request already sent");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IncomingFriendRequestAlreadyExistsException when incoming request already pending")
    void shouldThrowWhenIncomingRequestExists() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userBId, "Hello");
        when(userService.getById(userAId)).thenReturn(userA);
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userAId, userBId, FriendRequestStatus.PENDING))
                .thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userBId, userAId, FriendRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(userAId, dto))
                .isInstanceOf(IncomingFriendRequestAlreadyExistsException.class)
                .hasMessageContaining("pending friend request from this user already exists");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully send friend request")
    void shouldSendFriendRequestSuccessfully() {
        SendFriendRequestDto dto = new SendFriendRequestDto(userBId, "Let's be friends");
        when(userService.getById(userAId)).thenReturn(userA);
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userAId, userBId, FriendRequestStatus.PENDING))
                .thenReturn(false);
        when(friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(userBId, userAId, FriendRequestStatus.PENDING))
                .thenReturn(false);

        FriendRequest saved = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .message("Let's be friends")
                .status(FriendRequestStatus.PENDING)
                .build();
        saved.setId(UUID.randomUUID());
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(saved);

        FriendRequestResponseDto expectedDto = new FriendRequestResponseDto(
                saved.getId(), userAId, userA.getEmail(), "Alice", null,
                userBId, userB.getEmail(), "Bob", null,
                FriendRequestStatus.PENDING, "Let's be friends", Instant.now(), Instant.now()
        );
        when(friendshipMapper.toFriendRequestResponseDto(eq(saved), any(), any())).thenReturn(expectedDto);

        FriendRequestResponseDto result = friendshipService.sendFriendRequest(userAId, dto);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(FriendRequestStatus.PENDING);
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    @Test
    @DisplayName("Should throw FriendRequestAccessDeniedException when non-receiver accepts request")
    void shouldThrowWhenNonReceiverAccepts() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .status(FriendRequestStatus.PENDING)
                .build();
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(userAId, requestId))
                .isInstanceOf(FriendRequestAccessDeniedException.class)
                .hasMessageContaining("Only the receiver can accept");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidFriendRequestStateException when accepting non-pending request")
    void shouldThrowWhenAcceptingNonPendingRequest() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .status(FriendRequestStatus.ACCEPTED)
                .build();
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(userBId, requestId))
                .isInstanceOf(InvalidFriendRequestStateException.class)
                .hasMessageContaining("Only PENDING requests can be accepted");
    }

    @Test
    @DisplayName("Should accept friend request and create friendship")
    void shouldAcceptFriendRequestSuccessfully() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .status(FriendRequestStatus.PENDING)
                .build();
        request.setId(requestId);
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendRequestRepository.save(request)).thenReturn(request);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(false);

        FriendRequestResponseDto expected = new FriendRequestResponseDto(
                requestId, userAId, "userA@tourhive.com", null, null,
                userBId, "userB@tourhive.com", null, null,
                FriendRequestStatus.ACCEPTED, null, Instant.now(), Instant.now()
        );
        when(friendshipMapper.toFriendRequestResponseDto(eq(request), any(), any())).thenReturn(expected);

        FriendRequestResponseDto result = friendshipService.acceptFriendRequest(userBId, requestId);

        assertThat(result.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("Should decline friend request")
    void shouldDeclineFriendRequest() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .status(FriendRequestStatus.PENDING)
                .build();
        request.setId(requestId);
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenReturn(request);

        FriendRequestResponseDto expected = new FriendRequestResponseDto(
                requestId, userAId, "userA@tourhive.com", null, null,
                userBId, "userB@tourhive.com", null, null,
                FriendRequestStatus.DECLINED, null, Instant.now(), Instant.now()
        );
        when(friendshipMapper.toFriendRequestResponseDto(eq(request), any(), any())).thenReturn(expected);

        FriendRequestResponseDto result = friendshipService.declineFriendRequest(userBId, requestId);

        assertThat(result.status()).isEqualTo(FriendRequestStatus.DECLINED);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel friend request by sender")
    void shouldCancelFriendRequestBySender() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
                .sender(userA)
                .receiver(userB)
                .status(FriendRequestStatus.PENDING)
                .build();
        request.setId(requestId);
        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenReturn(request);

        FriendRequestResponseDto expected = new FriendRequestResponseDto(
                requestId, userAId, "userA@tourhive.com", null, null,
                userBId, "userB@tourhive.com", null, null,
                FriendRequestStatus.CANCELLED, null, Instant.now(), Instant.now()
        );
        when(friendshipMapper.toFriendRequestResponseDto(eq(request), any(), any())).thenReturn(expected);

        FriendRequestResponseDto result = friendshipService.cancelFriendRequest(userAId, requestId);

        assertThat(result.status()).isEqualTo(FriendRequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should check friendship status accurately")
    void shouldCheckFriendshipStatusAccurately() {
        when(userService.getById(userBId)).thenReturn(userB);
        when(userBlockService.isBlockedBetween(userAId, userBId)).thenReturn(false);
        when(friendshipRepository.areFriends(userAId, userBId)).thenReturn(true);

        FriendshipStatusResponseDto status = friendshipService.getFriendshipStatus(userAId, userBId);

        assertThat(status.status()).isEqualTo(FriendshipStatus.FRIENDS);
    }

    @Test
    @DisplayName("Should unfriend target user")
    void shouldUnfriendUser() {
        when(userService.getById(userBId)).thenReturn(userB);

        boolean aFirst = userAId.toString().compareTo(userBId.toString()) < 0;
        UUID u1 = aFirst ? userAId : userBId;
        UUID u2 = aFirst ? userBId : userAId;

        Friendship friendship = new Friendship(userA, userB);
        when(friendshipRepository.findByUser1IdAndUser2Id(u1, u2)).thenReturn(Optional.of(friendship));

        friendshipService.unfriend(userAId, userBId);

        verify(friendshipRepository).delete(friendship);
    }
}
