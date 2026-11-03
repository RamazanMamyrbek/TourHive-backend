package com.tourhive.backend.services.users;

import com.tourhive.backend.dto.users.request.CreateCertificateRequestDto;
import com.tourhive.backend.dto.users.response.CertificateDto;

import java.util.List;
import java.util.UUID;

public interface UserCertificateService {

    List<CertificateDto> getUserCertificates(UUID userId);

    CertificateDto addCertificate(UUID userId, CreateCertificateRequestDto request);

    void deleteCertificate(UUID userId, UUID certificateId);
}

