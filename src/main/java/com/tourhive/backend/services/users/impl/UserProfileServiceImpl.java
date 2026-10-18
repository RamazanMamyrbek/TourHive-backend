package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.dto.users.request.UpdateAvatarRequestDto;
import com.tourhive.backend.dto.users.request.UpdateProfileRequestDto;
import com.tourhive.backend.dto.users.response.PublicUserProfileResponseDto;
import com.tourhive.backend.dto.users.response.UserProfileResponseDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.NicknameAlreadyTakenException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.mappers.users.UserProfileMapper;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.users.UserProfileService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private final com.tourhive.backend.services.storage.FileStorageService fileStorageService;

    @Override
    @Transactional
    public UserProfileResponseDto getMyProfile(UUID userId) {
        User user = userService.getById(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder().user(user).build()));
        return userProfileMapper.toResponseDto(profile, user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(UUID userId, UpdateProfileRequestDto dto) {
        User user = userService.getById(userId);

        if (StringUtils.hasText(dto.nickname())) {
            String trimmedNickname = dto.nickname().trim();
            if (userProfileRepository.existsByNicknameAndUserIdNot(trimmedNickname, userId)) {
                throw new NicknameAlreadyTakenException(trimmedNickname);
            }
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        userProfileMapper.updateProfileFromDto(dto, profile);
        if (dto.nickname() != null) {
            profile.setNickname(StringUtils.hasText(dto.nickname()) ? dto.nickname().trim() : null);
        }

        UserProfile saved = userProfileRepository.save(profile);
        return userProfileMapper.toResponseDto(saved, user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateAvatar(UUID userId, UpdateAvatarRequestDto dto) {
        User user = userService.getById(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        profile.setAvatarUrl(dto.avatarUrl());
        UserProfile saved = userProfileRepository.save(profile);
        return userProfileMapper.toResponseDto(saved, user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.tourhive.backend.exceptions.InvalidFileException("Cannot upload an empty avatar file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.tourhive.backend.exceptions.InvalidFileException("Avatar must be an image file (JPEG, PNG, WebP)");
        }

        User user = userService.getById(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (StringUtils.hasText(profile.getAvatarUrl())) {
            fileStorageService.deleteFile(profile.getAvatarUrl());
        }

        String avatarUrl = fileStorageService.uploadFile(file, "avatars");
        profile.setAvatarUrl(avatarUrl);
        UserProfile saved = userProfileRepository.save(profile);
        return userProfileMapper.toResponseDto(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserProfileResponseDto getPublicProfile(UUID requestingUserId, UUID targetUserId) {
        if (requestingUserId != null && userBlockRepository.isBlockedBetween(requestingUserId, targetUserId)) {
            throw new ResourceNotFoundException("User not found: " + targetUserId);
        }

        User targetUser = userService.getById(targetUserId);
        UserProfile profile = userProfileRepository.findByUserId(targetUserId)
                .orElseGet(() -> UserProfile.builder().user(targetUser).build());

        return userProfileMapper.toPublicResponseDto(profile, targetUser);
    }
}
