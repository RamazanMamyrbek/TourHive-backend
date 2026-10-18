package com.tourhive.backend.services.storage.impl;

import com.tourhive.backend.configurations.minio.MinioProperties;
import com.tourhive.backend.exceptions.FileStorageException;
import com.tourhive.backend.exceptions.InvalidFileException;
import com.tourhive.backend.services.storage.FileStorageService;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String safeFolder = StringUtils.hasText(folder) ? folder.trim().replaceAll("^/+|/+$", "") : "general";
        String objectName = safeFolder + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                            .build()
            );

            String fileUrl = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
            log.info("Uploaded file to MinIO: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        try {
            String bucketPrefix = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/";
            if (fileUrl.startsWith(bucketPrefix)) {
                String objectName = fileUrl.substring(bucketPrefix.length());
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectName)
                                .build()
                );
                log.info("Deleted file from MinIO: {}", objectName);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", fileUrl, e);
        }
    }
}
