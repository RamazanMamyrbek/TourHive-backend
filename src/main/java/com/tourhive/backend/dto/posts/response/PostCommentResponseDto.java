package com.tourhive.backend.dto.posts.response;

import java.time.Instant;
import java.util.UUID;

public record PostCommentResponseDto(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorName,
        String authorNickname,
        String authorAvatarUrl,
        String content,
        boolean isMyComment,
        Instant createdAt
) {
}

