package com.tourhive.backend.services.users.impl;

import com.tourhive.backend.dto.users.request.CreateCertificateRequestDto;
import com.tourhive.backend.dto.users.response.CertificateDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserCertificate;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.TourAccessDeniedException;
import com.tourhive.backend.repositories.users.UserCertificateRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.users.UserCertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCertificateServiceImpl implements UserCertificateService {

    private final UserCertificateRepository certificateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDto> getUserCertificates(UUID userId) {
        return certificateRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CertificateDto addCertificate(UUID userId, CreateCertificateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserCertificate cert = UserCertificate.builder()
                .user(user)
                .title(request.title())
                .fileUrl(request.fileUrl())
                .fileName(request.fileName())
                .fileType(request.fileType())
                .issuer(request.issuer())
                .issueYear(request.issueYear())
                .build();

        UserCertificate saved = certificateRepository.save(cert);
        log.info("Certificate added for user {}: id={}, title={}", userId, saved.getId(), saved.getTitle());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCertificate(UUID userId, UUID certificateId) {
        UserCertificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        if (!cert.getUser().getId().equals(userId)) {
            throw new TourAccessDeniedException("You can only delete your own certificates");
        }

        certificateRepository.delete(cert);
        log.info("Certificate {} deleted by user {}", certificateId, userId);
    }

    private CertificateDto toDto(UserCertificate cert) {
        return new CertificateDto(
                cert.getId(),
                cert.getUser().getId(),
                cert.getTitle(),
                cert.getFileUrl(),
                cert.getFileName(),
                cert.getFileType(),
                cert.getIssuer(),
                cert.getIssueYear(),
                cert.getCreatedAt()
        );
    }
}

