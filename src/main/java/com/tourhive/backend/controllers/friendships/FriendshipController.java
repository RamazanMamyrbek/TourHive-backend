package com.tourhive.backend.controllers.friendships;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.friendships.request.SendFriendRequestDto;
import com.tourhive.backend.dto.friendships.response.FriendRequestResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendSummaryResponseDto;
import com.tourhive.backend.dto.friendships.response.FriendshipStatusResponseDto;
import com.tourhive.backend.services.friendships.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Friends", description = "Endpoints for managing friend requests, mutual friendships, and friendship status")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Operation(summary = "Send a friend request", description = "Sends a new friend request to another user", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/requests")
    public ResponseEntity<ApiResponseDto<FriendRequestResponseDto>> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendFriendRequestDto dto
    ) {
        FriendRequestResponseDto response = friendshipService.sendFriendRequest(principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Friend request sent successfully", response));
    }

    @Operation(summary = "Get incoming friend requests", description = "Returns pending friend requests received by current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponseDto<Page<FriendRequestResponseDto>>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<FriendRequestResponseDto> response = friendshipService.getIncomingRequests(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(summary = "Get outgoing friend requests", description = "Returns friend requests sent by current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/requests/outgoing")
    public ResponseEntity<ApiResponseDto<Page<FriendRequestResponseDto>>> getOutgoingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<FriendRequestResponseDto> response = friendshipService.getOutgoingRequests(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(summary = "Accept friend request", description = "Accepts a pending friend request and creates mutual friendship", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponseDto<FriendRequestResponseDto>> acceptFriendRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FriendRequestResponseDto response = friendshipService.acceptFriendRequest(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Friend request accepted", response));
    }

    @Operation(summary = "Decline friend request", description = "Declines a pending friend request", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<ApiResponseDto<FriendRequestResponseDto>> declineFriendRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FriendRequestResponseDto response = friendshipService.declineFriendRequest(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Friend request declined", response));
    }

    @Operation(summary = "Cancel friend request", description = "Cancels an outgoing pending friend request", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/requests/{id}/cancel")
    public ResponseEntity<ApiResponseDto<FriendRequestResponseDto>> cancelFriendRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FriendRequestResponseDto response = friendshipService.cancelFriendRequest(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("Friend request cancelled", response));
    }

    @Operation(summary = "Get friends list", description = "Returns paginated list of mutual friends of the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<FriendSummaryResponseDto>>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        Page<FriendSummaryResponseDto> response = friendshipService.getFriends(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(summary = "Check friendship status", description = "Returns current friendship status with the target user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<ApiResponseDto<FriendshipStatusResponseDto>> getFriendshipStatus(
            @PathVariable UUID targetUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FriendshipStatusResponseDto response = friendshipService.getFriendshipStatus(principal.getId(), targetUserId);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @Operation(summary = "Remove friend", description = "Removes target user from friends list", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> unfriend(
            @PathVariable UUID friendUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        friendshipService.unfriend(principal.getId(), friendUserId);
        return ResponseEntity.noContent().build();
    }
}
