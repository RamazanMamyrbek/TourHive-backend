package com.tourhive.backend.services.friendships.impl;

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
import com.tourhive.backend.exceptions.FriendRequestNotFoundException;
import com.tourhive.backend.exceptions.FriendshipNotFoundException;
import com.tourhive.backend.exceptions.IncomingFriendRequestAlreadyExistsException;
import com.tourhive.backend.exceptions.InvalidFriendRequestStateException;
import com.tourhive.backend.exceptions.UserBlockedException;
import com.tourhive.backend.mappers.friendships.FriendshipMapper;
import com.tourhive.backend.repositories.friendships.FriendRequestRepository;
import com.tourhive.backend.repositories.friendships.FriendshipRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.friendships.FriendshipService;
import com.tourhive.backend.services.users.UserBlockService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;
    private final UserBlockService userBlockService;
    private final FriendshipMapper friendshipMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(UUID senderId, SendFriendRequestDto dto) {
        UUID receiverId = dto.receiverId();

        if (senderId.equals(receiverId)) {
            throw new CannotBefriendSelfException("Cannot send friend request to yourself");
        }

        User sender = userService.getById(senderId);
        User receiver = userService.getById(receiverId);

        if (userBlockService.isBlockedBetween(senderId, receiverId)) {
            throw new UserBlockedException("Cannot send friend request: communication is blocked between users");
        }

        if (friendshipRepository.areFriends(senderId, receiverId)) {
            throw new AlreadyFriendsException("Users are already friends");
        }

        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, FriendRequestStatus.PENDING)) {
            throw new DuplicateFriendRequestException("Pending friend request already sent to this user");
        }

        if (friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(receiverId, senderId, FriendRequestStatus.PENDING)) {
            throw new IncomingFriendRequestAlreadyExistsException("A pending friend request from this user already exists. You can accept it instead.");
        }

        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .message(dto.message())
                .build();

        FriendRequest saved = friendRequestRepository.save(request);
        log.info("User {} sent friend request to user {}", senderId, receiverId);

        UserProfile senderProfile = userProfileRepository.findByUserId(senderId).orElse(null);
        UserProfile receiverProfile = userProfileRepository.findByUserId(receiverId).orElse(null);

        eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                receiverId,
                com.tourhive.backend.entities.notifications.NotificationType.FRIEND_REQUEST_RECEIVED,
                "New Friend Request",
                "You have received a friend request from " + (senderProfile != null && senderProfile.getFullName() != null ? senderProfile.getFullName() : sender.getEmail()),
                "FRIEND_REQUEST",
                saved.getId()
        ));

        return friendshipMapper.toFriendRequestResponseDto(saved, senderProfile, receiverProfile);
    }

    @Override
    public Page<FriendRequestResponseDto> getIncomingRequests(UUID userId, Pageable pageable) {
        Page<FriendRequest> requests = friendRequestRepository.findAllByReceiverIdAndStatusOrderByCreatedAtDesc(
                userId, FriendRequestStatus.PENDING, pageable
        );
        return requests.map(req -> {
            UserProfile senderProfile = userProfileRepository.findByUserId(req.getSender().getId()).orElse(null);
            UserProfile receiverProfile = userProfileRepository.findByUserId(req.getReceiver().getId()).orElse(null);
            return friendshipMapper.toFriendRequestResponseDto(req, senderProfile, receiverProfile);
        });
    }

    @Override
    public Page<FriendRequestResponseDto> getOutgoingRequests(UUID userId, Pageable pageable) {
        Page<FriendRequest> requests = friendRequestRepository.findAllBySenderIdOrderByCreatedAtDesc(userId, pageable);
        return requests.map(req -> {
            UserProfile senderProfile = userProfileRepository.findByUserId(req.getSender().getId()).orElse(null);
            UserProfile receiverProfile = userProfileRepository.findByUserId(req.getReceiver().getId()).orElse(null);
            return friendshipMapper.toFriendRequestResponseDto(req, senderProfile, receiverProfile);
        });
    }

    @Override
    @Transactional
    public FriendRequestResponseDto acceptFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found: " + requestId));

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new FriendRequestAccessDeniedException("Only the receiver can accept this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new InvalidFriendRequestStateException("Only PENDING requests can be accepted");
        }

        UUID senderId = request.getSender().getId();
        if (userBlockService.isBlockedBetween(senderId, currentUserId)) {
            throw new UserBlockedException("Cannot accept friend request: communication is blocked between users");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        FriendRequest saved = friendRequestRepository.save(request);

        if (!friendshipRepository.areFriends(senderId, currentUserId)) {
            boolean senderFirst = senderId.toString().compareTo(currentUserId.toString()) < 0;
            User u1 = senderFirst ? request.getSender() : request.getReceiver();
            User u2 = senderFirst ? request.getReceiver() : request.getSender();
            friendshipRepository.save(new Friendship(u1, u2));
            log.info("Friendship established between {} and {}", senderId, currentUserId);
        }

        UserProfile senderProfile = userProfileRepository.findByUserId(senderId).orElse(null);
        UserProfile receiverProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);

        eventPublisher.publishEvent(new com.tourhive.backend.events.NotificationEvent(
                senderId,
                com.tourhive.backend.entities.notifications.NotificationType.FRIEND_REQUEST_ACCEPTED,
                "Friend Request Accepted",
                (receiverProfile != null && receiverProfile.getFullName() != null ? receiverProfile.getFullName() : "A user") + " accepted your friend request",
                "FRIEND_REQUEST",
                saved.getId()
        ));

        return friendshipMapper.toFriendRequestResponseDto(saved, senderProfile, receiverProfile);
    }

    @Override
    @Transactional
    public FriendRequestResponseDto declineFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found: " + requestId));

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new FriendRequestAccessDeniedException("Only the receiver can decline this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new InvalidFriendRequestStateException("Only PENDING requests can be declined");
        }

        request.setStatus(FriendRequestStatus.DECLINED);
        FriendRequest saved = friendRequestRepository.save(request);
        log.info("User {} declined friend request {}", currentUserId, requestId);

        UserProfile senderProfile = userProfileRepository.findByUserId(request.getSender().getId()).orElse(null);
        UserProfile receiverProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        return friendshipMapper.toFriendRequestResponseDto(saved, senderProfile, receiverProfile);
    }

    @Override
    @Transactional
    public FriendRequestResponseDto cancelFriendRequest(UUID currentUserId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found: " + requestId));

        if (!request.getSender().getId().equals(currentUserId)) {
            throw new FriendRequestAccessDeniedException("Only the sender can cancel this friend request");
        }

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new InvalidFriendRequestStateException("Only PENDING requests can be cancelled");
        }

        request.setStatus(FriendRequestStatus.CANCELLED);
        FriendRequest saved = friendRequestRepository.save(request);
        log.info("User {} cancelled friend request {}", currentUserId, requestId);

        UserProfile senderProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
        UserProfile receiverProfile = userProfileRepository.findByUserId(request.getReceiver().getId()).orElse(null);
        return friendshipMapper.toFriendRequestResponseDto(saved, senderProfile, receiverProfile);
    }

    @Override
    public Page<FriendSummaryResponseDto> getFriends(UUID userId, Pageable pageable) {
        Page<Friendship> friendships = friendshipRepository.findAllByUserId(userId, pageable);
        return friendships.map(friendship -> {
            User friend = friendship.getUser1().getId().equals(userId) ? friendship.getUser2() : friendship.getUser1();
            UserProfile profile = userProfileRepository.findByUserId(friend.getId()).orElse(null);
            return friendshipMapper.toFriendSummaryResponseDto(friendship, friend, profile);
        });
    }

    @Override
    public FriendshipStatusResponseDto getFriendshipStatus(UUID currentUserId, UUID targetUserId) {
        userService.getById(targetUserId);

        if (currentUserId.equals(targetUserId)) {
            return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.NONE, null);
        }

        if (userBlockService.isBlockedBetween(currentUserId, targetUserId)) {
            return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.BLOCKED, null);
        }

        if (friendshipRepository.areFriends(currentUserId, targetUserId)) {
            return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.FRIENDS, null);
        }

        Optional<FriendRequest> outgoing = friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                currentUserId, targetUserId, FriendRequestStatus.PENDING
        );
        if (outgoing.isPresent()) {
            return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.PENDING_SENT, outgoing.get().getId());
        }

        Optional<FriendRequest> incoming = friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(
                targetUserId, currentUserId, FriendRequestStatus.PENDING
        );
        if (incoming.isPresent()) {
            return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.PENDING_RECEIVED, incoming.get().getId());
        }

        return new FriendshipStatusResponseDto(targetUserId, FriendshipStatus.NONE, null);
    }

    @Override
    @Transactional
    public void unfriend(UUID currentUserId, UUID friendUserId) {
        userService.getById(friendUserId);

        boolean currentFirst = currentUserId.toString().compareTo(friendUserId.toString()) < 0;
        UUID u1 = currentFirst ? currentUserId : friendUserId;
        UUID u2 = currentFirst ? friendUserId : currentUserId;

        Friendship friendship = friendshipRepository.findByUser1IdAndUser2Id(u1, u2)
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found with user: " + friendUserId));

        friendshipRepository.delete(friendship);
        log.info("User {} removed friend {}", currentUserId, friendUserId);
    }
}
