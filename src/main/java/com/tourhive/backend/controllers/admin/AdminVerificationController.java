package com.tourhive.backend.controllers.admin;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.admin.request.ReviewBusinessProfileRequestDto;
import com.tourhive.backend.dto.admin.response.BusinessProfileAdminResponseDto;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.services.admin.AdminVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin Business Verification", description = "Endpoints for moderating business verification applications")
@RestController
@RequestMapping("/api/v1/admin/business-verifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @Operation(summary = "List pending business verifications", description = "Returns a paginated list of applications awaiting review", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<BusinessProfileAdminResponseDto>>> getPendingVerifications(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<BusinessProfileAdminResponseDto> page = adminVerificationService.getPendingVerifications(pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }

    @Operation(summary = "Get business verification application details", description = "Returns full application data including legal details and documents", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<BusinessProfileAdminResponseDto>> getVerificationDetails(
            @PathVariable UUID id
    ) {
        BusinessProfileAdminResponseDto details = adminVerificationService.getVerificationDetails(id);
        return ResponseEntity.ok(ApiResponseDto.success(details));
    }

    @Operation(summary = "Review business verification application", description = "Approve or reject a business profile application", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponseDto<BusinessProfileAdminResponseDto>> reviewBusinessProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewBusinessProfileRequestDto request
    ) {
        BusinessProfileAdminResponseDto result = adminVerificationService.reviewBusinessProfile(
                principal.getId(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponseDto.success("Business profile reviewed successfully", result));
    }
}
