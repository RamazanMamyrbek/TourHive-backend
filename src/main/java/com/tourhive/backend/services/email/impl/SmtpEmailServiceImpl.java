package com.tourhive.backend.services.email.impl;

import com.tourhive.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@tourhive.com}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        log.info("Sending email verification code to {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("TourHive - Email Verification Code");
            message.setText("Welcome to TourHive!\n\nYour verification code is: " + code +
                    "\n\nThis code will expire in 15 minutes.\nIf you did not request this, please ignore this email.");

            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetToken(String toEmail, String resetToken) {
        log.info("Sending password reset instructions to {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("TourHive - Password Reset Request");
            message.setText("We received a request to reset your TourHive password.\n\nYour reset token is: " + resetToken +
                    "\n\nThis token will expire in 30 minutes.\nIf you did not request a password reset, please ignore this email.");

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
