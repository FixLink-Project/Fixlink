package com.fixlink.application.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
