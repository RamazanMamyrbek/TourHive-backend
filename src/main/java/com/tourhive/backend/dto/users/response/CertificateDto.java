package com.tourhive.backend.dto.users.response;

import java.time.Instant;
import java.util.UUID;

public record CertificateDto(
        UUID id,
        UUID userId,
        String title,
        String fileUrl,
        String fileName,
        String fileType,
        String issuer,
        String issueYear,
        Instant createdAt
) {
}

