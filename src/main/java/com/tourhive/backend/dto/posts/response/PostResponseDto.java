package com.tourhive.backend.dto.posts.response;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
        int commentsCount,
        boolean isLikedByMe,
        boolean isPrivate,
        List<PostMediaResponseDto> media,
        Instant createdAt
) {
    public PostResponseDto(
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
        this(id, authorId, authorName, authorNickname, authorAvatarUrl, imageUrl, caption, location,
                likesCount, 0, isLikedByMe, isPrivate, Collections.emptyList(), createdAt);
    }
}
