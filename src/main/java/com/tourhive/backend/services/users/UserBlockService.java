package com.tourhive.backend.services.users;

import com.tourhive.backend.dto.users.response.BlockedUserResponseDto;

import java.util.List;
import java.util.UUID;

public interface UserBlockService {

    void blockUser(UUID blockerId, UUID targetUserId);

    void unblockUser(UUID blockerId, UUID targetUserId);

    List<BlockedUserResponseDto> getBlockedUsers(UUID blockerId);

    boolean isBlockedBetween(UUID userA, UUID userB);
}
