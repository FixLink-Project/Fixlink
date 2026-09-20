package com.fixlink.infrastructure.service;

import com.fixlink.application.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = "https://fixlink.vn/reset-password?token=" + resetToken;
        log.info("Sending password reset email to: {} with URL: {}", toEmail, resetUrl);
        log.info("JavaMailSender not available; reset link generated: {}", resetUrl);
    }
}
