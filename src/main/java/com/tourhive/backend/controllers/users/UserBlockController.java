package com.tourhive.backend.controllers.users;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.users.response.BlockedUserResponseDto;
import com.tourhive.backend.services.users.UserBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "User Blocks", description = "Endpoints for managing user blocking and viewing blocked users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @Operation(summary = "Block a user", description = "Blocks target user, preventing profile and tour interactions", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponseDto<Void>> blockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userBlockService.blockUser(principal.getId(), id);
        return ResponseEntity.ok(ApiResponseDto.success("User blocked successfully", null));
    }

    @Operation(summary = "Unblock a user", description = "Removes block for target user", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/block")
    public ResponseEntity<Void> unblockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userBlockService.unblockUser(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get list of blocked users", description = "Returns all users blocked by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/blocks")
    public ResponseEntity<ApiResponseDto<List<BlockedUserResponseDto>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<BlockedUserResponseDto> blockedUsers = userBlockService.getBlockedUsers(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(blockedUsers));
    }
}
