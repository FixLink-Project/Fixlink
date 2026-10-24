package com.fixlink.chatbot.adapter.out.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.adapter.out.llm.ChatReplyParser;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResilientGeminiChatAdapterTest {

    private ResilientGeminiChatAdapter adapter;
    private ChatbotProperties properties;
    private ChatbotToolPort toolPort;
    private InMemoryRagIndex ragIndex;
    private ExtractiveFallbackSynthesizer fallbackSynthesizer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new ChatbotProperties();
        properties.getGemini().setApiKey("dummy-test-key");
        properties.getGemini().setModels(List.of("invalid-test-model"));

        toolPort = mock(ChatbotToolPort.class);
        ragIndex = mock(InMemoryRagIndex.class);
        fallbackSynthesizer = new ExtractiveFallbackSynthesizer();
        objectMapper = new ObjectMapper();

        adapter = new ResilientGeminiChatAdapter(
                properties,
                toolPort,
                fallbackSynthesizer,
                new ChatReplyParser(objectMapper),
                objectMapper
        );
    }

    @Test
    @DisplayName("Lọc sạch các liên kết URL độc hại ngoài danh mục cho phép")
    void shouldSanitizeMaliciousLinks() {
        String dirtyText = "Bấm vào đây để nhận quà: [Trúng thưởng](http://hacker-phishing.com/steal) hoặc [Đăng ký](/dang-ky).";
        String cleaned = adapter.sanitizeOutputLinks(dirtyText);

        assertThat(cleaned).doesNotContain("http://hacker-phishing.com");
        assertThat(cleaned).contains("[Đăng ký](/dang-ky)");
    }

    @Test
    @DisplayName("Tự động chuyển sang Fallback trích xuất cục bộ khi API lỗi")
    void shouldFallbackToLocalSynthesizerOnError() {
        LlmChatPort.ChatOrchestrationResult result = adapter.generateChatResponse(
                "System prompt",
                List.of(),
                "Quy trình tìm thợ thế nào?",
                List.of(),
                1L,
                "CUSTOMER",
                "/",
                null,
                "### Mô hình hoạt động (Nguồn: gioi-thieu-fixlink.md)\nCác thợ gửi báo giá, khách so sánh và chọn thợ."
        );

        assertThat(result).isNotNull();
        assertThat(result.modelUsed()).startsWith("local-");
        assertThat(result.reply()).isNotEmpty();
    }
}
