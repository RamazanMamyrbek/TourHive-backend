package com.tourhive.backend.services.email;

public interface EmailService {

    void sendVerificationCode(String toEmail, String code);

    void sendPasswordResetToken(String toEmail, String resetToken);
}
