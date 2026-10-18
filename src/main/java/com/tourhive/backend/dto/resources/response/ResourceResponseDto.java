package com.tourhive.backend.dto.resources.response;

import com.tourhive.backend.entities.resources.ResourceType;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponseDto(
        UUID id,
        String name,
        String folder,
        Long size,
        ResourceType contentType,
        String mimeType,
        String url,
        Instant createdAt
) {
}
