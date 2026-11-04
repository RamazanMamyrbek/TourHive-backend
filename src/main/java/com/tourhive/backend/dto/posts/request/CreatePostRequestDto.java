package com.tourhive.backend.dto.posts.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequestDto(
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        @Size(max = 2000, message = "Caption must not exceed 2000 characters")
        String caption,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        Boolean isPrivate
) {
    public CreatePostRequestDto(String imageUrl, String caption, String location) {
        this(imageUrl, caption, location, false);
    }
}

