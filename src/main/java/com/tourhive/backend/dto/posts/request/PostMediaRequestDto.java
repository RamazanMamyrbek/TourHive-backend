package com.tourhive.backend.dto.posts.request;

import jakarta.validation.constraints.NotBlank;

public record PostMediaRequestDto(
        @NotBlank(message = "Media URL is required")
        String mediaUrl,
        String mediaType
) {
    public PostMediaRequestDto(String mediaUrl) {
        this(mediaUrl, "IMAGE");
    }
}
