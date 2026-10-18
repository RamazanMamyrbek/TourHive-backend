package com.tourhive.backend.dto.resources.response;

import org.springframework.core.io.InputStreamResource;

public record FileDto(
        InputStreamResource file,
        String name,
        String contentType
) {
}
