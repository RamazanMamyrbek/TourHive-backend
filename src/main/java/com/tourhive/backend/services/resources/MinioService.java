package com.tourhive.backend.services.resources;

import com.tourhive.backend.entities.resources.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface MinioService {

    InputStream downloadFile(String key);

    void deleteFile(String key);

    Resource uploadFile(MultipartFile file, String folder);
}
