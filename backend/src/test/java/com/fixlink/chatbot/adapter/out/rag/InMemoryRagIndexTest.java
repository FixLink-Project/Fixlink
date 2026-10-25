package com.fixlink.chatbot.adapter.out.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRagIndexTest {

    private DocumentContextRegistry contextRegistry;
    private MarkdownDocumentLoader loader;
    private ChatbotProperties properties;
    private InMemoryRagIndex ragIndex;

    @BeforeEach
    void setUp() {
        contextRegistry = new DocumentContextRegistry();
        loader = new MarkdownDocumentLoader(contextRegistry);
        properties = new ChatbotProperties();
        properties.getRag().setTopK(3);
        properties.getRag().setMinScore(0.01);
        properties.getRag().setWindow(1);

        ragIndex = new InMemoryRagIndex(loader, contextRegistry, properties);
        ragIndex.reindex();
    }

    @Test
    @DisplayName("Nạp đầy đủ toàn bộ tài liệu Markdown vào index")
    void shouldLoadAndIndexKnowledgeChunks() {
        int count = ragIndex.getIndexedCount();
        assertThat(count).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Truy xuất theo từ khóa BM25 về chính sách đặt cọc và hoàn tiền")
    void shouldRetrieveDepositPolicyViaKeyword() {
        InMemoryRagIndex.RetrievalResult result = ragIndex.retrieve("đặt cọc tiền mua linh kiện", "customer");
        assertThat(result.mergedContent()).isNotEmpty();
        assertThat(result.mergedContent().toLowerCase()).contains("đặt cọc");
        assertThat(result.sources()).isNotEmpty();
    }

    @Test
    @DisplayName("Truy xuất mô hình báo giá cạnh tranh – khách chọn thợ")
    void shouldRetrieveQuotationModel() {
        InMemoryRagIndex.RetrievalResult result = ragIndex.retrieve("Thợ gửi báo giá thế nào và khách chọn thợ ra sao?", "customer");
        assertThat(result.mergedContent()).isNotEmpty();
        assertThat(result.mergedContent().toLowerCase()).containsAnyOf("chọn thợ này", "báo giá");
    }

    @Test
    @DisplayName("Lọc theo đối tượng người dùng (audience filtering)")
    void shouldRespectAudienceFilter() {
        // Technician query
        InMemoryRagIndex.RetrievalResult techResult = ragIndex.retrieve("Quy trình thợ xác minh hồ sơ và duyệt CCCD", "technician");
        assertThat(techResult.mergedContent()).isNotEmpty();

        // Guest query
        InMemoryRagIndex.RetrievalResult guestResult = ragIndex.retrieve("Giới thiệu nền tảng FixLink", "guest");
        assertThat(guestResult.mergedContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Cơ chế mở rộng cửa sổ ngữ cảnh window retrieval i±1")
    void shouldExpandContextWindow() {
        InMemoryRagIndex.RetrievalResult result = ragIndex.retrieve("quy trình xử lý sự cố thường gặp", "customer");
        assertThat(result.mergedContent()).isNotEmpty();
        // Result contains multiple section headers due to i±1 expansion
        assertThat(result.mergedContent()).contains("### ");
    }
}
