package com.tourhive.backend.services.resources.impl;

import com.tourhive.backend.dto.resources.response.FileDto;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.exceptions.InvalidFileException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.repositories.resources.ResourceRepository;
import com.tourhive.backend.services.resources.MinioService;
import com.tourhive.backend.services.resources.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ResourceRepository resourceRepository;
    private final MinioService minioService;

    public ResourceServiceImpl(ResourceRepository resourceRepository, MinioService minioService) {
        this.resourceRepository = resourceRepository;
        this.minioService = minioService;
    }

    @Override
    public FileDto getFileDtoById(UUID resourceId) {
        Resource resource = getResourceById(resourceId);
        InputStream inputStream = minioService.downloadFile(resource.getKey());
        String contentType = resource.getMimeType() != null ? resource.getMimeType() : getMimeType(resource.getContentType());
        return new FileDto(
                new InputStreamResource(inputStream),
                resource.getName(),
                contentType
        );
    }

    @Override
    public Resource getResourceById(UUID resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(
                () -> new ResourceNotFoundException("Resource not found by id: " + resourceId)
        );
    }

    @Override
    @Transactional
    public Resource saveResource(MultipartFile file, String folder) {
        Resource resource = minioService.uploadFile(file, folder);
        return resourceRepository.save(resource);
    }

    @Override
    @Transactional
    public Resource updateResource(UUID resourceId, MultipartFile file) {
        Resource oldResource = getResourceById(resourceId);
        minioService.deleteFile(oldResource.getKey());
        Resource newResource = minioService.uploadFile(file, oldResource.getFolder());
        oldResource.setName(newResource.getName());
        oldResource.setKey(newResource.getKey());
        oldResource.setSize(newResource.getSize());
        oldResource.setContentType(newResource.getContentType());
        oldResource.setMimeType(newResource.getMimeType());
        return resourceRepository.save(oldResource);
    }

    @Override
    @Transactional
    public void deleteResource(UUID resourceId) {
        Resource resource = getResourceById(resourceId);
        minioService.deleteFile(resource.getKey());
        resourceRepository.delete(resource);
    }

    @Override
    public void checkImage(MultipartFile file) {
        if (file == null) {
            log.error("File is null");
            throw new InvalidFileException("File is null");
        }
        if (file.isEmpty()) {
            log.error("File is empty");
            throw new InvalidFileException("File is empty");
        }
        if (file.getOriginalFilename() == null) {
            log.error("File has no original filename");
            throw new InvalidFileException("File has no original filename");
        }
        if (file.getContentType() == null) {
            log.error("File has no content type");
            throw new InvalidFileException("File has no content type");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType().toLowerCase())) {
            log.error("Invalid file type: {}", file.getContentType());
            throw new InvalidFileException("Invalid file type. Submit only image/png, image/jpeg, image/webp");
        }
    }

    @Override
    public void checkFile(MultipartFile file) {
        if (file == null) {
            log.error("File is null");
            throw new InvalidFileException("File is null");
        }
        if (file.isEmpty()) {
            log.error("File is empty");
            throw new InvalidFileException("File is empty");
        }
        if (file.getOriginalFilename() == null) {
            log.error("File has no original filename");
            throw new InvalidFileException("File has no original filename");
        }
        if (file.getContentType() == null) {
            log.error("File has no content type");
            throw new InvalidFileException("File has no content type");
        }
        if (!ALLOWED_DOCUMENT_TYPES.contains(file.getContentType().toLowerCase())) {
            log.error("Invalid file type: {}", file.getContentType());
            throw new InvalidFileException("Invalid file type. Submit only image/png, image/jpeg, image/webp, application/pdf, doc, docx");
        }
    }

    private String getMimeType(ResourceType contentType) {
        return switch (contentType) {
            case IMAGE -> "image/png";
            case VIDEO -> "video/mp4";
            case AUDIO -> "audio/mpeg";
            case PDF -> "application/pdf";
            case MSWORD -> "application/msword";
            case MSEXCEL -> "application/vnd.ms-excel";
            case ZIP -> "application/zip";
            case TEXT -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
