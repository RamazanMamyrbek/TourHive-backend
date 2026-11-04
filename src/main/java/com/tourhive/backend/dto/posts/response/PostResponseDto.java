package com.tourhive.backend.dto.posts.response;

import java.time.Instant;
import java.util.UUID;

public record PostResponseDto(
        UUID id,
        UUID authorId,
        String authorName,
        String authorNickname,
        String authorAvatarUrl,
        String imageUrl,
        String caption,
        String location,
        int likesCount,
        boolean isLikedByMe,
        boolean isPrivate,
        Instant createdAt
) {
}

