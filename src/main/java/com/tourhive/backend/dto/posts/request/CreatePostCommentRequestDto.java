package com.tourhive.backend.dto.posts.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostCommentRequestDto(
        @NotBlank(message = "Comment content cannot be blank")
        @Size(max = 1000, message = "Comment content cannot exceed 1000 characters")
        String content
) {
}
