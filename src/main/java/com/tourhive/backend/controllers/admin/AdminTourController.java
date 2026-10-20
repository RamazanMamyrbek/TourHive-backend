package com.tourhive.backend.controllers.admin;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.admin.request.ReviewTourRequestDto;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.services.admin.AdminTourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "Admin Tour Moderation", description = "Endpoints for reviewing and moderating submitted tours")
@RestController
@RequestMapping("/api/v1/admin/tours")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AdminTourController {

    private final AdminTourService adminTourService;

    @Operation(summary = "List pending tours for moderation", description = "Returns a paginated list of tours awaiting publication review", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/pending")
    public ResponseEntity<ApiResponseDto<Page<TourResponseDto>>> getPendingTours(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TourResponseDto> page = adminTourService.getPendingTours(pageable);
        return ResponseEntity.ok(ApiResponseDto.success(page));
    }

    @Operation(summary = "Review tour publication application", description = "Approve (PUBLISHED) or reject (REJECTED) a submitted tour", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> reviewTour(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewTourRequestDto request
    ) {
        TourResponseDto result = adminTourService.reviewTour(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Tour reviewed successfully", result));
    }
}
