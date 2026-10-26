package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.application.service.ToolCallRecorder.ToolTrace;
import com.fixlink.chatbot.domain.model.ChatCard;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.TicketPriority;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatEngagementFeaturesTest {

    private final ChatCardFactory cardFactory = new ChatCardFactory(new ObjectMapper());

    @Test
    @DisplayName("Thẻ đơn sửa chữa có thanh tiến độ đúng bước và nhãn cảnh báo khi khách cần nghiệm thu")
    void shouldBuildRequestCardWithProgress() {
        String json = """
                {"id":26,"requestCode":"REQ-20261023-A1B2C3","title":"Sửa tủ lạnh","categoryName":"Sửa Chữa Điện Lạnh",
                 "status":"AWAITING_ACCEPTANCE","statusLabel":"Chờ khách nghiệm thu","agreedPriceText":"400.000 đ",
                 "depositText":"120.000 đ","technicianName":"Lê Minh","nextStep":"Kiểm tra rồi bấm Nghiệm thu.",
                 "detailUrl":"/yeu-cau-cua-toi/26","appointments":[]}
                """;
        List<ChatCard> cards = cardFactory.fromToolTraces(List.of(new ToolTrace("get_repair_request_status", json, false)));

        assertThat(cards).hasSize(1);
        ChatCard card = cards.get(0);
        assertThat(card.getCurrentStep()).isEqualTo(4);
        assertThat(card.getSteps()).hasSize(6);
        assertThat(card.getBadgeTone()).isEqualTo("warning");
        assertThat(card.getUrl()).isEqualTo("/yeu-cau-cua-toi/26");
        assertThat(card.getFields()).extracting(ChatCard.Field::getLabel).contains("Giá chốt", "Tiền cọc (30%)", "Thợ phụ trách");
    }

    @Test
    @DisplayName("Không dựng thẻ khi tool báo cần đăng nhập hoặc lỗi; đơn hủy không có thanh tiến độ")
    void shouldSkipLoginRequiredAndCancelledProgress() {
        List<ChatCard> none = cardFactory.fromToolTraces(List.of(
                new ToolTrace("get_my_repair_requests", "{\"requireLogin\":true}", false),
                new ToolTrace("get_my_appointments", "{\"isError\":true}", true)));
        assertThat(none).isEmpty();

        List<ChatCard> cancelled = cardFactory.fromToolTraces(List.of(new ToolTrace("get_my_repair_requests",
                "{\"viewerRole\":\"CUSTOMER\",\"items\":[{\"id\":5,\"title\":\"Sửa khóa\",\"status\":\"CANCELLED\",\"statusLabel\":\"Đã hủy\"}]}",
                false)));
        assertThat(cancelled.get(0).getSteps()).isNull();
        assertThat(cancelled.get(0).getBadgeTone()).isEqualTo("danger");
    }

    @Test
    @DisplayName("Bộ nhớ đệm chỉ lưu câu hỏi công khai ở lượt đầu, không lưu dữ liệu cá nhân hay câu trả lời dự phòng")
    void shouldCacheOnlyPublicFirstTurnAnswers() {
        FaqAnswerCache cache = new FaqAnswerCache();
        ChatOrchestrationResult llm = new ChatOrchestrationResult("FixLink là...", List.of(), List.of(), false, null, "openai/gpt-oss-120b");

        cache.store("FixLink là gì?", "GUEST", List.of(), llm, List.of());
        assertThat(cache.lookup("fixlink la gi", "GUEST", List.of())).isNotNull();
        assertThat(cache.lookup("FixLink là gì?", "GUEST", List.of()).result().modelUsed()).endsWith("(cache)");

        cache.store("Đơn của tôi?", "CUSTOMER", List.of(), llm,
                List.of(new ToolTrace("get_my_repair_requests", "{}", false)));
        assertThat(cache.lookup("Đơn của tôi?", "CUSTOMER", List.of())).isNull();

        ChatOrchestrationResult local = new ChatOrchestrationResult("...", List.of(), List.of(), false, null, "local-faq");
        cache.store("Cọc bao nhiêu?", "GUEST", List.of(), local, List.of());
        assertThat(cache.lookup("Cọc bao nhiêu?", "GUEST", List.of())).isNull();

        List<ChatMessage> history = List.of(ChatMessage.builder().role("user").content("hi").build());
        assertThat(cache.lookup("FixLink là gì?", "GUEST", history)).isNull();
    }

    @Test
    @DisplayName("Phiếu chuyển nhân viên tự suy ra mức ưu tiên theo nội dung")
    void shouldInferHandoffPriority() {
        assertThat(ChatbotEngagementService.inferPriority("Tôi bị trừ tiền 2 lần, nghi lừa đảo")).isEqualTo(TicketPriority.CRITICAL);
        assertThat(ChatbotEngagementService.inferPriority("Thợ không đến mà không báo")).isEqualTo(TicketPriority.HIGH);
        assertThat(ChatbotEngagementService.inferPriority("Muốn hỏi thêm về quy trình đăng ký")).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    @DisplayName("Phát hiện khách hỏi lại gần như nguyên văn câu trước")
    void shouldDetectRepeatedQuestion() {
        assertThat(ChatbotOrchestratorService.similarity("tôi muốn biết tiền cọc tính thế nào vậy",
                "tiền cọc tính thế nào vậy tôi muốn biết")).isGreaterThanOrEqualTo(0.8);
        assertThat(ChatbotOrchestratorService.similarity("tiền cọc tính thế nào vậy",
                "thợ không đến thì làm sao")).isLessThan(0.5);
    }
}
