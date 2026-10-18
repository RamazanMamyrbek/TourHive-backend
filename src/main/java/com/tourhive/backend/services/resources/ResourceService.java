package com.tourhive.backend.services.resources;

import com.tourhive.backend.dto.resources.response.FileDto;
import com.tourhive.backend.entities.resources.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ResourceService {

    FileDto getFileDtoById(UUID resourceId);

    Resource getResourceById(UUID resourceId);

    Resource saveResource(MultipartFile file, String folder);

    Resource updateResource(UUID resourceId, MultipartFile file);

    void deleteResource(UUID resourceId);

    void checkImage(MultipartFile file);

    void checkFile(MultipartFile file);
}
