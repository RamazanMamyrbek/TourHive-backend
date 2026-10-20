package com.tourhive.backend.controllers.tours;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.CancelApplicationRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.dto.tours.response.TourApplicationResponseDto;
import com.tourhive.backend.dto.tours.response.TourApplicationShortResponseDto;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.services.tours.TourApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Tour Applications", description = "Endpoints for applying to tours, managing participants and review workflows")
@RestController
@RequestMapping("/api/v1/tours/{tourId}/applications")
public class TourApplicationController {

    private final TourApplicationService tourApplicationService;

    public TourApplicationController(TourApplicationService tourApplicationService) {
        this.tourApplicationService = tourApplicationService;
    }

    @Operation(summary = "Apply for a tour", description = "Submits a participation application for a published tour", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponseDto<TourApplicationResponseDto>> applyForTour(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) ApplyTourRequestDto request
    ) {
        TourApplicationResponseDto application = tourApplicationService.applyForTour(tourId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Application submitted successfully", application));
    }

    @Operation(summary = "Get applications for tour", description = "Returns paginated applications for a specific tour (organizer only)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<TourApplicationShortResponseDto>>> getTourApplications(
            @PathVariable UUID tourId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional filter by application status")
            @RequestParam(required = false) TourApplicationStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TourApplicationShortResponseDto> applications = tourApplicationService.getTourApplications(tourId, principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(applications));
    }

    @Operation(summary = "Get application details by ID", description = "Returns full application details including status history", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<TourApplicationResponseDto>> getApplicationById(
            @PathVariable UUID tourId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TourApplicationResponseDto application = tourApplicationService.getApplicationById(tourId, id, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(application));
    }

    @Operation(summary = "Review tour application", description = "Accepts or rejects a pending application (organizer only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponseDto<TourApplicationResponseDto>> reviewApplication(
            @PathVariable UUID tourId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewApplicationRequestDto request
    ) {
        TourApplicationResponseDto application = tourApplicationService.reviewApplication(tourId, id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Application reviewed successfully", application));
    }

    @Operation(summary = "Withdraw or cancel participation", description = "Traveler withdraws a pending application or cancels an accepted participation", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponseDto<TourApplicationResponseDto>> cancelApplication(
            @PathVariable UUID tourId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) CancelApplicationRequestDto request
    ) {
        TourApplicationResponseDto application = tourApplicationService.cancelApplication(tourId, id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Application cancelled successfully", application));
    }
}
