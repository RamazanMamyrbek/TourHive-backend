package com.tourhive.backend.dto.posts.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequestDto(
        String imageUrl,

        @Size(max = 2000, message = "Caption must not exceed 2000 characters")
        String caption,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        Boolean isPrivate,

        List<PostMediaRequestDto> media
) {
    public CreatePostRequestDto(String imageUrl, String caption, String location, Boolean isPrivate) {
        this(imageUrl, caption, location, isPrivate, null);
    }

    public CreatePostRequestDto(String imageUrl, String caption, String location) {
        this(imageUrl, caption, location, false, null);
    }
}
