package com.tourhive.backend.resources;

import com.tourhive.backend.dto.resources.response.FileDto;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.exceptions.InvalidFileException;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.repositories.resources.ResourceRepository;
import com.tourhive.backend.services.resources.MinioService;
import com.tourhive.backend.services.resources.impl.ResourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private UUID resourceId;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        testResource = new Resource(
                "avatar.png",
                "avatars/uuid.png",
                "avatars",
                2048L,
                ResourceType.IMAGE,
                "image/png"
        );
        testResource.setId(resourceId);
    }

    @Test
    @DisplayName("getFileDtoById should return FileDto with input stream and metadata")
    void shouldGetFileDtoById() {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));
        when(minioService.downloadFile(testResource.getKey()))
                .thenReturn(new ByteArrayInputStream("image content".getBytes()));

        FileDto fileDto = resourceService.getFileDtoById(resourceId);

        assertThat(fileDto).isNotNull();
        assertThat(fileDto.name()).isEqualTo("avatar.png");
        assertThat(fileDto.contentType()).isEqualTo("image/png");
        assertThat(fileDto.file()).isNotNull();
    }

    @Test
    @DisplayName("getFileDtoById should throw ResourceNotFoundException when id does not exist")
    void shouldThrowWhenResourceNotFound() {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getFileDtoById(resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(resourceId.toString());
    }

    @Test
    @DisplayName("saveResource should call minioService and save to repository")
    void shouldSaveResource() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "photo data".getBytes()
        );

        when(minioService.uploadFile(file, "avatars")).thenReturn(testResource);
        when(resourceRepository.save(testResource)).thenReturn(testResource);

        Resource saved = resourceService.saveResource(file, "avatars");

        assertThat(saved).isEqualTo(testResource);
        verify(minioService).uploadFile(file, "avatars");
        verify(resourceRepository).save(testResource);
    }

    @Test
    @DisplayName("deleteResource should remove from MinIO and delete database entity")
    void shouldDeleteResource() {
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));

        resourceService.deleteResource(resourceId);

        verify(minioService).deleteFile(testResource.getKey());
        verify(resourceRepository).delete(testResource);
    }

    @Test
    @DisplayName("checkImage should pass for valid JPEG, PNG, WebP")
    void shouldPassCheckImageForValidFormats() {
        MockMultipartFile jpeg = new MockMultipartFile("file", "pic.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile png = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        MockMultipartFile webp = new MockMultipartFile("file", "pic.webp", "image/webp", "data".getBytes());

        resourceService.checkImage(jpeg);
        resourceService.checkImage(png);
        resourceService.checkImage(webp);
    }

    @Test
    @DisplayName("checkImage should throw InvalidFileException for PDF or empty file")
    void shouldThrowCheckImageForInvalidFormat() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
        MockMultipartFile empty = new MockMultipartFile("file", "pic.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> resourceService.checkImage(pdf))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid file type");

        assertThatThrownBy(() -> resourceService.checkImage(empty))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File is empty");
    }
}
