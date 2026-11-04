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
    private final com.tourhive.backend.services.resources.ResourceService resourceService;

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
        if (dto.firstName() != null) {
            profile.setFirstName(StringUtils.hasText(dto.firstName()) ? dto.firstName().trim() : null);
        }
        if (dto.lastName() != null) {
            profile.setLastName(StringUtils.hasText(dto.lastName()) ? dto.lastName().trim() : null);
        }
        if (dto.nickname() != null) {
            profile.setNickname(StringUtils.hasText(dto.nickname()) ? dto.nickname().trim() : null);
        }
        if (dto.bio() != null) {
            profile.setBio(StringUtils.hasText(dto.bio()) ? dto.bio().trim() : null);
        }
        if (dto.phone() != null) {
            profile.setPhone(StringUtils.hasText(dto.phone()) ? dto.phone().trim() : null);
        }
        if (dto.country() != null) {
            profile.setCountry(StringUtils.hasText(dto.country()) ? dto.country().trim() : null);
        }
        if (dto.city() != null) {
            profile.setCity(StringUtils.hasText(dto.city()) ? dto.city().trim() : null);
        }
        if (dto.languages() != null) {
            profile.setLanguages(StringUtils.hasText(dto.languages()) ? dto.languages().trim() : null);
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

        if (profile.getAvatarResource() != null && (dto.avatarUrl() == null || !dto.avatarUrl().contains(profile.getAvatarResource().getId().toString()))) {
            resourceService.deleteResource(profile.getAvatarResource().getId());
            profile.setAvatarResource(null);
        }

        profile.setAvatarUrl(dto.avatarUrl());
        UserProfile saved = userProfileRepository.save(profile);
        return userProfileMapper.toResponseDto(saved, user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        resourceService.checkImage(file);

        User user = userService.getById(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (profile.getAvatarResource() != null) {
            resourceService.deleteResource(profile.getAvatarResource().getId());
        }

        com.tourhive.backend.entities.resources.Resource newResource = resourceService.saveResource(file, "avatars");
        profile.setAvatarResource(newResource);
        profile.setAvatarUrl("/api/v1/resources/" + newResource.getId());
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

    @Override
    @Transactional(readOnly = true)
    public PublicUserProfileResponseDto getPublicProfile(UUID requestingUserId, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("User not found");
        }

        if ("me".equalsIgnoreCase(identifier)) {
            if (requestingUserId == null) {
                throw new ResourceNotFoundException("User not found: me");
            }
            return getPublicProfile(requestingUserId, requestingUserId);
        }

        if (identifier.contains("@")) {
            User user = userService.getByEmail(identifier.trim().toLowerCase());
            return getPublicProfile(requestingUserId, user.getId());
        }

        try {
            UUID targetUserId = UUID.fromString(identifier);
            try {
                return getPublicProfile(requestingUserId, targetUserId);
            } catch (ResourceNotFoundException e) {
                var profileOpt = userProfileRepository.findById(targetUserId);
                if (profileOpt.isPresent()) {
                    return getPublicProfile(requestingUserId, profileOpt.get().getUser().getId());
                }
                throw e;
            }
        } catch (IllegalArgumentException e) {
            User user = userService.getByEmail(identifier.trim().toLowerCase());
            return getPublicProfile(requestingUserId, user.getId());
        }
    }
}
