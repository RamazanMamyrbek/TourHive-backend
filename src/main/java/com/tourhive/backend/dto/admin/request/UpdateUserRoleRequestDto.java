package com.tourhive.backend.dto.admin.request;

import com.tourhive.backend.entities.users.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update user role")
public record UpdateUserRoleRequestDto(
        @NotNull(message = "Role is required")
        @Schema(description = "New user role", example = "MODERATOR")
        Role role
) {
}
