package com.tourhive.backend.services.users;

import com.tourhive.backend.dto.users.response.FollowStatusResponseDto;
import com.tourhive.backend.dto.users.response.UserFollowSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserFollowService {

    FollowStatusResponseDto follow(UUID currentUserId, UUID targetUserId);

    FollowStatusResponseDto unfollow(UUID currentUserId, UUID targetUserId);

    FollowStatusResponseDto getFollowStatus(UUID currentUserId, UUID targetUserId);

    Page<UserFollowSummaryDto> getFollowers(UUID targetUserId, UUID currentUserId, Pageable pageable);

    Page<UserFollowSummaryDto> getFollowing(UUID targetUserId, UUID currentUserId, Pageable pageable);
}
