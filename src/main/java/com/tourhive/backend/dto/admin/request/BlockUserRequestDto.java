package com.tourhive.backend.dto.admin.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to block a user account")
public record BlockUserRequestDto(
        @NotBlank(message = "Block reason cannot be blank")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        @Schema(description = "Explanation for the account block", example = "Multiple verified fraud reports")
        String reason
) {
}
