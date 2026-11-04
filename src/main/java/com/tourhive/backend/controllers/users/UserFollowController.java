package com.tourhive.backend.controllers.users;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.users.response.FollowStatusResponseDto;
import com.tourhive.backend.dto.users.response.UserFollowSummaryDto;
import com.tourhive.backend.services.users.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "User Follows", description = "Endpoints for following/unfollowing users and followers lists")
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    @Operation(summary = "Follow user", description = "Follows target user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/follow")
    public ResponseEntity<ApiResponseDto<FollowStatusResponseDto>> follow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FollowStatusResponseDto response = userFollowService.follow(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponseDto.success("Followed successfully", response));
    }

    @Operation(summary = "Unfollow user", description = "Unfollows target user", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/follow")
    public ResponseEntity<ApiResponseDto<FollowStatusResponseDto>> unfollow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FollowStatusResponseDto response = userFollowService.unfollow(principal.getId(), userId);
        return ResponseEntity.ok(ApiResponseDto.success("Unfollowed successfully", response));
    }

    @Operation(summary = "Get follow status", description = "Returns follow status and followers count")
    @GetMapping("/follow-status")
    public ResponseEntity<ApiResponseDto<FollowStatusResponseDto>> getFollowStatus(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        FollowStatusResponseDto status = userFollowService.getFollowStatus(currentUserId, userId);
        return ResponseEntity.ok(ApiResponseDto.success(status));
    }

    @Operation(summary = "Get user followers", description = "Returns paginated list of user's followers")
    @GetMapping("/followers")
    public ResponseEntity<ApiResponseDto<Page<UserFollowSummaryDto>>> getFollowers(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        Page<UserFollowSummaryDto> page = userFollowService.getFollowers(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }

    @Operation(summary = "Get user following", description = "Returns paginated list of users that this user follows")
    @GetMapping("/following")
    public ResponseEntity<ApiResponseDto<Page<UserFollowSummaryDto>>> getFollowing(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        Page<UserFollowSummaryDto> page = userFollowService.getFollowing(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }
}

