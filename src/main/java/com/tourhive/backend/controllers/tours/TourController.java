package com.tourhive.backend.controllers.tours;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.tours.request.AddTourImageRequestDto;
import com.tourhive.backend.dto.tours.request.CancelTourRequestDto;
import com.tourhive.backend.dto.tours.request.CreateTourRequestDto;
import com.tourhive.backend.dto.tours.request.TourSearchCriteria;
import com.tourhive.backend.dto.tours.request.UpdateTourRequestDto;
import com.tourhive.backend.dto.tours.response.TourImageResponseDto;
import com.tourhive.backend.dto.tours.response.TourResponseDto;
import com.tourhive.backend.dto.tours.response.TourShortResponseDto;
import com.tourhive.backend.services.tours.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tours", description = "Endpoints for creating, managing, publishing and searching tours")
@RestController
@RequestMapping("/api/v1/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @Operation(summary = "Search published tours catalog", description = "Public catalog of tours with dynamic filters, search, pagination and sorting")
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<TourShortResponseDto>>> searchTours(
            @ModelAttribute @ParameterObject TourSearchCriteria criteria,
            @ParameterObject
            @PageableDefault(size = 20, sort = "startDate", direction = org.springframework.data.domain.Sort.Direction.ASC) Pageable pageable
    ) {
        Page<TourShortResponseDto> tours = tourService.searchPublishedTours(criteria, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(tours));
    }

    @Operation(summary = "Submit tour for publication", description = "Submits draft tour for moderation review", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> publishTour(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TourResponseDto tour = tourService.publishTour(id, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Tour submitted for publication review", tour));
    }

    @Operation(summary = "Cancel tour", description = "Cancels a published or pending tour by organizer", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> cancelTour(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CancelTourRequestDto request
    ) {
        TourResponseDto tour = tourService.cancelTour(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Tour cancelled successfully", tour));
    }

    @Operation(summary = "Create tour draft", description = "Creates a new COMMUNITY or COMMERCIAL tour draft", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/draft")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> createDraft(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTourRequestDto request
    ) {
        TourResponseDto tour = tourService.createDraft(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Tour draft created successfully", tour));
    }

    @Operation(summary = "Get current organizer's drafts", description = "Returns a list of tour drafts created by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-drafts")
    public ResponseEntity<ApiResponseDto<List<TourShortResponseDto>>> getMyDrafts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TourShortResponseDto> drafts = tourService.getMyDrafts(principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(drafts));
    }

    @Operation(summary = "Get tour details by ID", description = "Returns full details of a tour (drafts accessible only to owner/moderator)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> getTourById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        TourResponseDto tour = tourService.getTourById(id, currentUserId);
        return ResponseEntity.ok(ApiResponseDto.success(tour));
    }

    @Operation(summary = "Update tour draft", description = "Updates details, route stops, and parameters of an existing draft", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<TourResponseDto>> updateDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTourRequestDto request
    ) {
        TourResponseDto updated = tourService.updateDraft(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success("Tour draft updated successfully", updated));
    }

    @Operation(summary = "Delete tour draft", description = "Deletes a draft tour and its associated stops and images", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteTour(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tourService.deleteTour(id, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Tour draft deleted successfully", null));
    }

    @Operation(summary = "Attach image to tour", description = "Attaches a previously uploaded Resource to the tour", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponseDto<TourImageResponseDto>> addTourImage(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddTourImageRequestDto request
    ) {
        TourImageResponseDto image = tourService.addTourImage(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Image attached to tour successfully", image));
    }

    @Operation(summary = "Remove image from tour", description = "Removes an image from the tour", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ApiResponseDto<Void>> removeTourImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tourService.removeTourImage(id, imageId, principal.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Image removed from tour successfully", null));
    }
}
