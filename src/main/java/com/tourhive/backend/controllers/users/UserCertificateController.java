package com.tourhive.backend.controllers.users;

import com.tourhive.backend.configurations.security.UserPrincipal;
import com.tourhive.backend.dto.common.ApiResponseDto;
import com.tourhive.backend.dto.users.request.CreateCertificateRequestDto;
import com.tourhive.backend.dto.users.response.CertificateDto;
import com.tourhive.backend.services.users.UserCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "User Certificates", description = "Endpoints for managing user certifications, diplomas, and licenses")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserCertificateController {

    private final UserCertificateService certificateService;

    @Operation(summary = "Get user certificates", description = "Returns all published certificates and credentials for a given user")
    @GetMapping("/{userId}/certificates")
    public ResponseEntity<ApiResponseDto<List<CertificateDto>>> getUserCertificates(@PathVariable UUID userId) {
        List<CertificateDto> certificates = certificateService.getUserCertificates(userId);
        return ResponseEntity.ok(ApiResponseDto.success(certificates));
    }

    @Operation(summary = "Add certificate for current user", description = "Adds a certificate, diploma or credential to the current user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/me/certificates")
    public ResponseEntity<ApiResponseDto<CertificateDto>> addCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCertificateRequestDto request
    ) {
        CertificateDto created = certificateService.addCertificate(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Certificate added successfully", created));
    }

    @Operation(summary = "Delete certificate", description = "Deletes a certificate belonging to the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me/certificates/{certificateId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCertificate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID certificateId
    ) {
        certificateService.deleteCertificate(principal.getId(), certificateId);
        return ResponseEntity.ok(ApiResponseDto.success("Certificate deleted successfully", null));
    }
}

