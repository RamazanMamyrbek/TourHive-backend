package com.tourhive.backend.configurations.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfiguration {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build()
            );
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build()
                );
                log.info("Created MinIO bucket: {}", minioProperties.getBucketName());

                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": ["*"]},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(minioProperties.getBucketName());

                client.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .config(policy)
                                .build()
                );
                log.info("Set public read policy for MinIO bucket: {}", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            log.warn("MinIO bucket check deferred or unavailable: {}", e.getMessage());
        }

        return client;
    }
}
