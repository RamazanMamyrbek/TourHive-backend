package com.tourhive.backend.services.friendships;

import com.tourhive.backend.dto.friendships.request.SendFriendRequestDto;
import com.tourhive.backend.dto.friendships.response.FriendRequestResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendSummaryResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendshipStatusResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FriendshipService {

    FriendRequestResponseDto sendFriendRequest(UUID senderId, SendFriendRequestDto dto);

    Page<FriendRequestResponseDto> getIncomingRequests(UUID userId, Pageable pageable);

    Page<FriendRequestResponseDto> getOutgoingRequests(UUID userId, Pageable pageable);

    FriendRequestResponseDto acceptFriendRequest(UUID currentUserId, UUID requestId);

    FriendRequestResponseDto declineFriendRequest(UUID currentUserId, UUID requestId);

    FriendRequestResponseDto cancelFriendRequest(UUID currentUserId, UUID requestId);

    Page<FriendSummaryResponseDto> getFriends(UUID userId, Pageable pageable);

    FriendshipStatusResponseDto getFriendshipStatus(UUID currentUserId, UUID targetUserId);

    void unfriend(UUID currentUserId, UUID friendUserId);
}
