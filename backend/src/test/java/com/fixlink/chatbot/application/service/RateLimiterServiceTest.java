package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import com.fixlink.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;
    private ChatbotProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ChatbotProperties();
        properties.getLimits().setGuestRatePer10min(5);

        rateLimiterService = new RateLimiterService(properties);
    }

    @Test
    @DisplayName("Cho phép người dùng gửi tin trong hạn mức không ném ngoại lệ")
    void shouldAllowRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> rateLimiterService.checkRateLimit("192.168.1.1", null))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Chặn khi khách vượt quá giới hạn (Rate limit exceeded)")
    void shouldBlockWhenGuestLimitExceeded() {
        String clientIp = "10.0.0.1";
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkRateLimit(clientIp, null);
        }
        // 6th request should fail
        assertThatThrownBy(() -> rateLimiterService.checkRateLimit(clientIp, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn đã gửi quá nhiều tin nhắn");
    }

    @Test
    @DisplayName("Người dùng đã đăng nhập có hạn mức cao hơn khách vãng lai")
    void shouldGiveHigherLimitToAuthenticatedUser() {
        Long userId = 42L;
        // User has limit of 60
        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> rateLimiterService.checkRateLimit("10.0.0.2", userId))
                    .doesNotThrowAnyException();
        }
    }
}
