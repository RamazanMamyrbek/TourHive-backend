package com.tourhive.backend.dto.media.response;

public record FileUploadResponseDto(
        String fileUrl,
        String fileName,
        String contentType,
        long size
) {
}
