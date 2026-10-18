package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.dto.users.response.BlockedUserResponseDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserBlock;
import com.tourhive.backend.entities.users.UserProfile;
import com.tourhive.backend.exceptions.CannotBlockSelfException;
import com.tourhive.backend.repositories.users.UserBlockRepository;
import com.tourhive.backend.repositories.users.UserProfileRepository;
import com.tourhive.backend.services.users.UserBlockService;
import com.tourhive.backend.services.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void blockUser(UUID blockerId, UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new CannotBlockSelfException("Users cannot block themselves");
        }

        User blocker = userService.getById(blockerId);
        User blocked = userService.getById(targetUserId);

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            log.debug("User {} already blocked user {}", blockerId, targetUserId);
            return;
        }

        UserBlock userBlock = UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        userBlockRepository.save(userBlock);
        log.info("User {} blocked user {}", blockerId, targetUserId);
    }

    @Override
    @Transactional
    public void unblockUser(UUID blockerId, UUID targetUserId) {
        userBlockRepository.findByBlockerIdAndBlockedId(blockerId, targetUserId)
                .ifPresent(block -> {
                    userBlockRepository.delete(block);
                    log.info("User {} unblocked user {}", blockerId, targetUserId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponseDto> getBlockedUsers(UUID blockerId) {
        List<UserBlock> blocks = userBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc(blockerId);
        return blocks.stream()
                .map(block -> {
                    UserProfile profile = userProfileRepository.findByUserId(block.getBlocked().getId()).orElse(null);
                    return new BlockedUserResponseDto(
                            block.getId(),
                            block.getBlocked().getId(),
                            block.getBlocked().getEmail(),
                            profile != null ? profile.getNickname() : null,
                            profile != null ? profile.getAvatarUrl() : null,
                            block.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBetween(UUID userA, UUID userB) {
        if (userA == null || userB == null) {
            return false;
        }
        return userBlockRepository.isBlockedBetween(userA, userB);
    }
}
