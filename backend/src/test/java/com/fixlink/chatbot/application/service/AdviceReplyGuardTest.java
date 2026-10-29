package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdviceReplyGuardTest {

    @Test
    @DisplayName("Khách xin tư vấn mà AI chỉ hỏi lại có muốn đặt thợ: coi là bỏ qua phần tư vấn")
    void shouldDetectRepliesWithoutAdvice() {
        assertThat(ChatbotOrchestratorService.skipsAdvice("Bạn muốn đăng yêu cầu sửa nồi cơm điện ngay bây giờ?")).isTrue();
        assertThat(ChatbotOrchestratorService.skipsAdvice("")).isTrue();
        assertThat(ChatbotOrchestratorService.skipsAdvice("""
                Nồi không lên đèn thường do mất nguồn hoặc hỏng cầu chì nhiệt. Trước khi gọi thợ:
                1. Thử cắm sang ổ điện khác.
                2. Kiểm tra dây nguồn có bị đứt, gập.""")).isFalse();
        assertThat(ChatbotOrchestratorService.skipsAdvice("- Rút phích cắm\n- Lau khô và hứng nước")).isFalse();
    }
}
