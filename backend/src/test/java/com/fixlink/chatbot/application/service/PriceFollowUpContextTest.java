package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fixlink.chatbot.adapter.out.gemini.ExtractiveFallbackSynthesizer;
import com.fixlink.chatbot.domain.model.ChatMessage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceFollowUpContextTest {

    @org.junit.jupiter.api.BeforeEach
    void resetLiveCatalog() {
        com.fixlink.chatbot.domain.service.LiveCatalog.reset();
    }

    private final ExtractiveFallbackSynthesizer synthesizer = new ExtractiveFallbackSynthesizer();

    private static ChatMessage user(String text) {
        return ChatMessage.builder().role("user").content(text).build();
    }

    private static ChatMessage bot(String text) {
        return ChatMessage.builder().role("model").content(text).build();
    }

    private String answer(String message, List<ChatMessage> history) {
        String query = ChatbotOrchestratorService.withDeviceContext(message, history);
        return synthesizer.directAnswer(query).map(r -> r.reply()).orElse("");
    }

    @Test
    @DisplayName("Hỏi giá nối tiếp dùng thiết bị khách vừa kể, không đổ cả bảng giá")
    void shouldUseDeviceFromConversation() {
        List<ChatMessage> history = List.of(user("cửa cuốn kêu to mỗi lần kéo"), bot("Sự cố thuộc Sửa Khóa & Cửa Cuốn"));
        assertThat(answer("giá sửa bao nhiêu", history)).contains("350.000 đ").doesNotContain("Vệ sinh máy lạnh");

        List<ChatMessage> ctx = List.of(user("máy lạnh không lạnh"), bot("..."), user("nạp ga thì bao nhiêu"), bot("250.000 đ"));
        assertThat(answer("thế còn vệ sinh", ctx)).contains("150.000 đ").doesNotContain("Nạp ga");
    }

    @Test
    @DisplayName("Không gắn ngữ cảnh cho câu hỏi cọc, câu không hỏi giá hoặc khi chưa có thiết bị")
    void shouldLeaveOtherMessagesUntouched() {
        List<ChatMessage> history = List.of(user("tủ lạnh kêu to"));
        assertThat(ChatbotOrchestratorService.withDeviceContext("cọc bao nhiêu", history)).isEqualTo("cọc bao nhiêu");
        assertThat(ChatbotOrchestratorService.withDeviceContext("cảm ơn", history)).isEqualTo("cảm ơn");
        assertThat(ChatbotOrchestratorService.withDeviceContext("giá bao nhiêu", List.of(user("chào bạn"))))
                .isEqualTo("giá bao nhiêu");
    }
}
