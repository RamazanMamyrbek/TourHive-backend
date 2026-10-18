package com.tourhive.backend.controllers.resources;

import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.resources.response.FileDto;
import com.tourhive.backend.dto.resources.response.ResourceResponseDto;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.mappers.resources.ResourceMapper;
import com.tourhive.backend.services.resources.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
@Validated
@Tag(name = "ResourceController", description = "Endpoints for file resources and media streaming")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceMapper resourceMapper;

    public ResourceController(ResourceService resourceService, ResourceMapper resourceMapper) {
        this.resourceService = resourceService;
        this.resourceMapper = resourceMapper;
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Get resource file content by ID", description = "Streams the stored file directly through TourHive backend")
    public ResponseEntity<InputStreamResource> getResource(
            @Parameter(description = "Resource ID") @PathVariable("resourceId") UUID resourceId
    ) {
        FileDto fileDto = resourceService.getFileDtoById(resourceId);
        String encodedFileName = URLEncoder.encode(fileDto.name(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"")
                .header("File-Name", encodedFileName)
                .contentType(MediaType.parseMediaType(fileDto.contentType()))
                .body(fileDto.file());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a resource file",
            description = "Uploads file to storage, creates a Resource database record, and returns its metadata and backend URL",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<ResourceResponseDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        Resource resource = resourceService.saveResource(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("File uploaded successfully", resourceMapper.toResponseDto(resource)));
    }
}
