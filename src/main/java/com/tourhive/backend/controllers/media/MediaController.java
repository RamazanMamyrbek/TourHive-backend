package com.tourhive.backend.controllers.media;

import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.media.response.FileUploadResponseDto;
import com.tourhive.backend.services.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Media", description = "Endpoints for uploading files, documents, and photos")
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload a file to object storage", description = "Uploads file (document, photo) to MinIO and returns its public URL", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDto<FileUploadResponseDto>> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        String url = fileStorageService.uploadFile(file, folder);
        FileUploadResponseDto response = new FileUploadResponseDto(
                url,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        return ResponseEntity.ok(ApiResponseDto.success("File uploaded successfully", response));
    }
}
