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
import java.util.List;
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
    private final com.tourhive.backend.repositories.chats.MessageRepository messageRepository;
    private final com.tourhive.backend.repositories.chats.ChatMemberRepository chatMemberRepository;

    public ResourceServiceImpl(
            ResourceRepository resourceRepository,
            MinioService minioService,
            com.tourhive.backend.repositories.chats.MessageRepository messageRepository,
            com.tourhive.backend.repositories.chats.ChatMemberRepository chatMemberRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.minioService = minioService;
        this.messageRepository = messageRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    @Override
    public FileDto getFileDtoById(UUID resourceId) {
        return getFileDtoById(resourceId, null);
    }

    @Override
    public FileDto getFileDtoById(UUID resourceId, UUID currentUserId) {
        Resource resource = getResourceById(resourceId);
        String folder = resource.getFolder();

        // Enforce access control for private chat attachments
        if ("chats".equalsIgnoreCase(folder)) {
            if (currentUserId == null) {
                throw new org.springframework.security.access.AccessDeniedException("Authentication required to access chat attachments");
            }
            List<com.tourhive.backend.entities.chats.Message> messages =
                    messageRepository.findAllByAttachmentUrlContaining(resourceId.toString());
            boolean isMember = false;
            for (com.tourhive.backend.entities.chats.Message msg : messages) {
                if (chatMemberRepository.existsByChatIdAndUserIdAndIsActiveTrue(msg.getChat().getId(), currentUserId)) {
                    isMember = true;
                    break;
                }
            }
            if (!isMember && !messages.isEmpty()) {
                throw new org.springframework.security.access.AccessDeniedException("User is not a participant of the chat containing this attachment");
            }
        }

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
        if ("tours".equalsIgnoreCase(folder) || "avatars".equalsIgnoreCase(folder) || "chats".equalsIgnoreCase(folder) || "posts".equalsIgnoreCase(folder)) {
            checkImage(file);
        } else {
            checkFile(file);
        }
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
        String contentType = file.getContentType();
        if (contentType == null || "application/octet-stream".equalsIgnoreCase(contentType)) {
            contentType = probeContentType(file.getOriginalFilename(), file);
        }
        if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            log.error("Invalid file type: {}", contentType);
            throw new InvalidFileException("Invalid file type. Submit only image/png, image/jpeg, image/webp");
        }
    }

    private String probeContentType(String filename, MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (read >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return "image/png";
            }
            if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                    header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "image/webp";
            }
        } catch (Exception ignored) {
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".webp")) return "image/webp";
        }
        return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
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
        String contentType = file.getContentType();
        if (contentType == null || "application/octet-stream".equalsIgnoreCase(contentType)) {
            contentType = probeContentType(file.getOriginalFilename(), file);
        }
        if (!ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            log.error("Invalid file type: {}", contentType);
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
