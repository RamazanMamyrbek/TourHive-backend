package com.tourhive.backend.dto.posts.response;

import java.util.UUID;

public record PostMediaResponseDto(
        UUID id,
        String mediaUrl,
        String mediaType
) {
}

