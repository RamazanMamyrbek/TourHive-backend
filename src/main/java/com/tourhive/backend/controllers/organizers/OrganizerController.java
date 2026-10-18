package com.tourhive.backend.controllers.organizers;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.organizers.request.RegisterBusinessProfileRequestDto;
import com.tourhive.backend.dto.organizers.response.BusinessProfileResponseDto;
import com.tourhive.backend.dto.organizers.response.OrganizerProfileResponseDto;
import com.tourhive.backend.services.organizers.OrganizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Organizers", description = "Endpoints for managing organizer profile and business verification")
@RestController
@RequestMapping("/api/v1/organizers")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    @Operation(summary = "Activate private organizer profile", description = "Activates organizer status for current user", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/me/activate")
    public ResponseEntity<ApiResponseDto<OrganizerProfileResponseDto>> activateOrganizer(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrganizerProfileResponseDto profile = organizerService.activateOrganizer(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Organizer profile activated successfully", profile));
    }

    @Operation(summary = "Get current organizer profile", description = "Returns organizer profile of the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<OrganizerProfileResponseDto>> getMyOrganizerProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrganizerProfileResponseDto profile = organizerService.getMyOrganizerProfile(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    @Operation(summary = "Submit business profile application", description = "Registers legal entity details and verification documents", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/me/business")
    public ResponseEntity<ApiResponseDto<BusinessProfileResponseDto>> registerBusinessProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterBusinessProfileRequestDto request
    ) {
        BusinessProfileResponseDto response = organizerService.registerBusinessProfile(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Business profile application submitted successfully", response));
    }

    @Operation(summary = "Get current business profile", description = "Returns business profile and verification status of current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/business")
    public ResponseEntity<ApiResponseDto<BusinessProfileResponseDto>> getMyBusinessProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BusinessProfileResponseDto profile = organizerService.getMyBusinessProfile(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }

    @Operation(summary = "Get public organizer profile", description = "Returns public organizer details and verified business info if available")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<OrganizerProfileResponseDto>> getPublicOrganizerProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID requestingUserId = principal != null ? principal.getId() : null;
        OrganizerProfileResponseDto profile = organizerService.getPublicOrganizerProfile(requestingUserId, id);
        return ResponseEntity.ok(ApiResponseDto.success(profile));
    }
}
