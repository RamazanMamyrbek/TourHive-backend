package com.tourhive.backend.controllers.users;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.users.request.UpdateAvatarRequestDto;
import com.tourhive.backend.dto.users.request.UpdateProfileRequestDto;
import com.tourhive.backend.dto.users.response.PublicUserProfileResponseDto;
import com.tourhive.backend.dto.users.response.UserProfileResponseDto;
import com.tourhive.backend.services.users.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;

@Tag(name = "Users", description = "Endpoints for user profiles and public user information")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final com.tourhive.backend.services.users.UserService userService;

    @Operation(summary = "Get current user profile", description = "Returns full profile of the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponseDto profile = userProfileService.getMyProfile(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    @Operation(summary = "Delete current user account", description = "Permanently deactivates and anonymizes account data", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteMyAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Account deleted successfully", null));
    }

    @Operation(summary = "Update current user profile", description = "Updates editable personal details of the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequestDto request
    ) {
        UserProfileResponseDto updated = userProfileService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Profile updated successfully", updated));
    }

    @Operation(summary = "Upload current user avatar (multipart)", description = "Uploads avatar image file directly to MinIO and updates user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        UserProfileResponseDto updated = userProfileService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(ApiResponseDto.success("Avatar uploaded successfully", updated));
    }

    @Operation(summary = "Update current user avatar URL (json)", description = "Sets or updates the avatar image URL directly", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAvatarRequestDto request
    ) {
        UserProfileResponseDto updated = userProfileService.updateAvatar(principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Avatar updated successfully", updated));
    }

    @Operation(summary = "Get public user profile", description = "Returns public profile data hiding sensitive private fields")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PublicUserProfileResponseDto>> getPublicProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID requestingUserId = principal != null ? principal.getId() : null;
        PublicUserProfileResponseDto profile = userProfileService.getPublicProfile(requestingUserId, id);
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }
}
