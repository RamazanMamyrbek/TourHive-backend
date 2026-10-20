package com.tourhive.backend.controllers.users;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationShortResponseDto;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.services.tours.TourApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Applications & Trips", description = "Endpoints for viewing user's own submitted applications and trips")
@RestController
@RequestMapping("/api/v1/users/me/applications")
public class UserApplicationController {

    private final TourApplicationService tourApplicationService;

    public UserApplicationController(TourApplicationService tourApplicationService) {
        this.tourApplicationService = tourApplicationService;
    }

    @Operation(summary = "Get my tour applications and trips", description = "Returns a paginated list of applications submitted by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<TourApplicationShortResponseDto>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional filter by application status")
            @RequestParam(required = false) TourApplicationStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TourApplicationShortResponseDto> applications = tourApplicationService.getMyApplications(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(applications));
    }
}
