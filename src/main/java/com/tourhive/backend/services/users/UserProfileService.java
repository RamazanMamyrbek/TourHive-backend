package com.tourhive.backend.services.users;

import com.tourhive.backend.dto.users.request.UpdateAvatarRequestDto;
import com.tourhive.backend.dto.users.request.UpdateProfileRequestDto;
import com.tourhive.backend.dto.users.response.PublicUserProfileResponseDto;
import com.tourhive.backend.dto.users.response.UserProfileResponseDto;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponseDto getMyProfile(UUID userId);

    UserProfileResponseDto updateProfile(UUID userId, UpdateProfileRequestDto dto);

    UserProfileResponseDto updateAvatar(UUID userId, UpdateAvatarRequestDto dto);

    PublicUserProfileResponseDto getPublicProfile(UUID requestingUserId, UUID targetUserId);
}
