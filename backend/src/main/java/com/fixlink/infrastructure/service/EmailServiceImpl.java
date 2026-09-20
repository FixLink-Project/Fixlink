package com.fixlink.infrastructure.service;

import com.fixlink.application.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chưa nối dịch vụ gửi thư thật: ở chế độ phát triển, liên kết đặt lại mật khẩu
 * được ghi ra log để lập trình viên mở trực tiếp.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    /**
     * Liên kết gần nhất theo từng email, chỉ giữ trong bộ nhớ để môi trường phát
     * triển và bài kiểm thử lấy lại được. Token trong cơ sở dữ liệu chỉ lưu bản
     * băm nên không thể suy ngược ra liên kết từ đó.
     */
    private final Map<String, String> lastResetLinks = new ConcurrentHashMap<>();

    private final String resetPasswordBaseUrl;

    public EmailServiceImpl(
            @Value("${app.frontend.reset-password-url:http://localhost:5173/dat-lai-mat-khau}")
            String resetPasswordBaseUrl
    ) {
        this.resetPasswordBaseUrl = resetPasswordBaseUrl;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = resetPasswordBaseUrl + "?token=" + resetToken;
        lastResetLinks.put(toEmail.toLowerCase(), resetUrl);
        log.info(">>> Liên kết đặt lại mật khẩu cho {}: {}", toEmail, resetUrl);
    }

    /** Dành cho môi trường phát triển và kiểm thử: lấy lại liên kết gần nhất. */
    public String getLastResetLink(String email) {
        return lastResetLinks.get(email.toLowerCase());
    }
}
