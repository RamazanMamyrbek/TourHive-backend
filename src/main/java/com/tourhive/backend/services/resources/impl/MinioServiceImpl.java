package com.tourhive.backend.services.resources.impl;

import com.tourhive.backend.configurations.minio.MinioProperties;
import com.tourhive.backend.entities.resources.Resource;
import com.tourhive.backend.entities.resources.ResourceType;
import com.tourhive.backend.exceptions.FileStorageException;
import com.tourhive.backend.services.resources.MinioService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public Resource uploadFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String safeFolder = StringUtils.hasText(folder) ? folder.trim().replaceAll("^/+|/+$", "") : "general";
        String key = safeFolder + "/" + UUID.randomUUID() + extension;
        String rawContentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(key)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(rawContentType)
                            .build()
            );

            log.info("Uploaded object to MinIO: key={}, size={}", key, file.getSize());

            Resource resource = new Resource();
            resource.setName(originalFilename);
            resource.setKey(key);
            resource.setFolder(safeFolder);
            resource.setSize(file.getSize());
            resource.setContentType(ResourceType.getResourceType(rawContentType));
            resource.setMimeType(rawContentType);
            return resource;
        } catch (Exception e) {
            log.error("Error occurred while uploading file to MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Error occurred while uploading file: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error occurred while downloading file from MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Error occurred while downloading file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(key)
                            .build()
            );
            log.info("Deleted object from MinIO: key={}", key);
        } catch (Exception e) {
            log.error("Error occurred while deleting file from MinIO: {}", e.getMessage(), e);
            throw new FileStorageException("Error occurred while deleting file: " + e.getMessage(), e);
        }
    }
}
