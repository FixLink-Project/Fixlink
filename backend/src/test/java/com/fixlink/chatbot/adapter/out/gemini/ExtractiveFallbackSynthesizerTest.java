package com.fixlink.chatbot.adapter.out.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractiveFallbackSynthesizerTest {

    private final ExtractiveFallbackSynthesizer synthesizer = new ExtractiveFallbackSynthesizer();

    @Test
    @DisplayName("Câu hỏi nguy hiểm luôn được ưu tiên hướng dẫn an toàn")
    void shouldPrioritizeSafety() {
        ChatOrchestrationResult r = synthesizer.synthesize("Ổ cắm nhà tôi đang bốc khói và có mùi khét", "");
        assertThat(r.reply()).contains("114").contains("cầu dao");
    }

    @Test
    @DisplayName("Hỏi hủy đơn trả về đúng quy định trạng thái được/không được hủy")
    void shouldAnswerCancellationRules() {
        ChatOrchestrationResult r = synthesizer.synthesize("Tôi muốn hủy đơn thì có được không?", "");
        assertThat(r.reply()).contains("IN_PROGRESS").contains("3 ngày");
    }

    @Test
    @DisplayName("Hỏi hotline không bịa số điện thoại, đề nghị tạo phiếu hỗ trợ")
    void shouldNotInventHotline() {
        ChatOrchestrationResult r = synthesizer.synthesize("Cho tôi xin số hotline của FixLink", "");
        assertThat(r.reply()).contains("đang được cập nhật").doesNotContainPattern("\\d{8,}");
        assertThat(r.needsHuman()).isTrue();
    }

    @Test
    @DisplayName("Câu hỏi khác chọn đúng một mục tri thức khớp nhất, không ghép tài liệu lung tung")
    void shouldExtractBestMatchingSection() {
        String rag = """
                ### Ảnh chứng từ KYC (Nguồn: anh-va-chung-tu.md)
                - Thợ cần tải ảnh mặt trước và mặt sau CCCD.

                ### Lịch hẹn (Nguồn: quy-dinh.md)
                - Hủy lịch hẹn bắt buộc nhập lý do hủy lịch hẹn.
                """;
        ChatOrchestrationResult r = synthesizer.synthesize("Hủy lịch hẹn có cần lý do không?", rag);
        assertThat(r.modelUsed()).isEqualTo("local-synthesizer");
        assertThat(r.reply()).contains("Lịch hẹn").doesNotContain("CCCD").doesNotContain("Nguồn:");
    }

    @Test
    @DisplayName("Tình huống thực tế khi hết hạn mức AI: đổi thợ, đòi tiền mặt, thiết bị viết tắt, báo hỏng chung chung")
    void shouldHandleRealWorldScenariosOffline() {
        assertThat(synthesizer.synthesize("tôi muốn đổi thợ khác được không", "").reply())
                .contains("không có tính năng chọn, chỉ định hay đổi thợ");
        assertThat(synthesizer.synthesize("thợ đòi thêm 500k tiền mặt", "").reply())
                .contains("không trả tiền mặt");
        assertThat(synthesizer.synthesize("tl nhà mình hư rồi", "").reply())
                .contains("Sửa Chữa Điện Lạnh").contains("200.000 đ");
        assertThat(synthesizer.synthesize("đồ nhà tôi bị hỏng", "").reply())
                .contains("Thiết bị nào");
        assertThat(synthesizer.synthesize("cho xin số điện thoại tổng đài", "").reply())
                .contains("đang được cập nhật");
    }
}
