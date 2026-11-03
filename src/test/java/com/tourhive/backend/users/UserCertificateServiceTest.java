package com.tourhive.backend.users;

import com.tourhive.backend.dto.users.request.CreateCertificateRequestDto;
import com.tourhive.backend.dto.users.response.CertificateDto;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserCertificate;
import com.tourhive.backend.exceptions.ResourceNotFoundException;
import com.tourhive.backend.exceptions.TourAccessDeniedException;
import com.tourhive.backend.repositories.users.UserCertificateRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.users.impl.UserCertificateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCertificateServiceTest {

    @Mock
    private UserCertificateRepository certificateRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCertificateServiceImpl certificateService;

    private User user;
    private UUID userId;
    private UserCertificate certificate;
    private UUID certId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().email("guide@tourhive.com").build();
        user.setId(userId);

        certId = UUID.randomUUID();
        certificate = UserCertificate.builder()
                .user(user)
                .title("Certified Mountain Guide")
                .fileUrl("/api/v1/resources/guide-cert.pdf")
                .fileName("guide-cert.pdf")
                .fileType("application/pdf")
                .issuer("Alpine Federation")
                .issueYear("2024")
                .build();
        certificate.setId(certId);
    }

    @Test
    @DisplayName("getUserCertificates should return mapped certificates list")
    void shouldGetUserCertificates() {
        when(certificateRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(certificate));

        List<CertificateDto> results = certificateService.getUserCertificates(userId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Certified Mountain Guide");
        assertThat(results.get(0).issuer()).isEqualTo("Alpine Federation");
        assertThat(results.get(0).fileUrl()).isEqualTo("/api/v1/resources/guide-cert.pdf");
    }

    @Test
    @DisplayName("addCertificate should save and return created certificate DTO")
    void shouldAddCertificate() {
        CreateCertificateRequestDto request = new CreateCertificateRequestDto(
                "Ski Instructor License",
                "/api/v1/resources/ski-cert.jpg",
                "ski-cert.jpg",
                "image/jpeg",
                "Ski Association",
                "2023"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(certificateRepository.save(any(UserCertificate.class))).thenAnswer(inv -> {
            UserCertificate saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CertificateDto result = certificateService.addCertificate(userId, request);

        assertThat(result.title()).isEqualTo("Ski Instructor License");
        assertThat(result.fileUrl()).isEqualTo("/api/v1/resources/ski-cert.jpg");
        assertThat(result.issuer()).isEqualTo("Ski Association");
        verify(certificateRepository).save(any(UserCertificate.class));
    }

    @Test
    @DisplayName("deleteCertificate should delete when current user is owner")
    void shouldDeleteCertificateWhenOwner() {
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));

        certificateService.deleteCertificate(userId, certId);

        verify(certificateRepository).delete(certificate);
    }

    @Test
    @DisplayName("deleteCertificate should throw TourAccessDeniedException when user is not owner")
    void shouldThrowWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));

        assertThatThrownBy(() -> certificateService.deleteCertificate(otherUserId, certId))
                .isInstanceOf(TourAccessDeniedException.class)
                .hasMessageContaining("You can only delete your own certificates");

        verify(certificateRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCertificate should throw ResourceNotFoundException when certificate not found")
    void shouldThrowWhenCertificateNotFound() {
        UUID notFoundId = UUID.randomUUID();
        when(certificateRepository.findById(notFoundId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.deleteCertificate(userId, notFoundId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Certificate not found");
    }
}

