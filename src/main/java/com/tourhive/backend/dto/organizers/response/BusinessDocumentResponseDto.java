package com.tourhive.backend.dto.organizers.response;

import java.util.UUID;

public record BusinessDocumentResponseDto(
        UUID id,
        String documentName,
        String documentUrl
) {
}
