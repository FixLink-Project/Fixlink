package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScenarioPlaybookTest {

    private final ScenarioPlaybook playbook = new ScenarioPlaybook();

    private String guidance(String message) {
        return String.join(" || ", playbook.guidanceFor(message, false));
    }

    @Test
    @DisplayName("Hiểu viết tắt, không dấu: 'ml ko lanh' là tư vấn máy lạnh")
    void shouldUnderstandAbbreviations() {
        assertThat(guidance("ml nha e ko lanh")).contains("TƯ VẤN ĐẶT THỢ");
        assertThat(ScenarioPlaybook.normalizeWithAbbreviations("tl hư r, sửa bn tiền"))
                .isEqualTo("tu lanh hu roi sua bao nhieu tien");
    }

    @Test
    @DisplayName("Báo hỏng mà chưa nói thiết bị thì hướng dẫn hỏi lại thay vì đoán")
    void shouldAskWhichDeviceWhenMissing() {
        assertThat(guidance("đồ nhà tôi bị hỏng rồi giúp với")).contains("CHƯA nói thiết bị");
    }

    @Test
    @DisplayName("Tình huống nguy hiểm luôn được ưu tiên trước mọi kịch bản khác")
    void shouldPrioritizeDanger() {
        List<String> result = playbook.guidanceFor("ổ cắm bốc khói, thợ thì chưa đến", false);
        assertThat(result.get(0)).contains("NGUY HIỂM");
    }

    @Test
    @DisplayName("'Gặp nhân viên' không bị hiểu nhầm là 'cần gấp'; 'gia dụng' không bị hiểu là hỏi giá")
    void shouldAvoidFalsePositives() {
        assertThat(guidance("cho tôi gặp nhân viên")).contains("hotline").doesNotContain("cần gấp");
        assertThat(guidance("sửa điện gia dụng gồm những gì")).doesNotContain("Hỏi giá");
    }

    @Test
    @DisplayName("Các tình huống thực tế: thợ không đến, đòi thêm tiền, đổi thợ, ngoài khu vực")
    void shouldMatchRealWorldScenarios() {
        assertThat(guidance("thợ hẹn 2h mà giờ thợ chưa đến")).contains("trễ hẹn");
        assertThat(guidance("thợ đòi thêm tiền mặt")).contains("ngoài hệ thống");
        assertThat(guidance("tôi muốn đổi thợ khác")).contains("KHÔNG đổi được thợ sau khi đã chọn");
        assertThat(guidance("đơn của tôi có mấy báo giá, nên chọn thợ nào")).contains("get_request_quotations");
        assertThat(guidance("ở Bình Dương có thợ không")).contains("chưa phục vụ");
    }

    @Test
    @DisplayName("Câu trả lời ngắn sau khi trợ lý vừa hỏi lại được hiểu là thông tin bổ sung")
    void shouldTreatShortReplyAsAnswerToPreviousQuestion() {
        assertThat(String.join(" ", playbook.guidanceFor("quận 7", true))).contains("câu trả lời cho câu hỏi bạn vừa hỏi");
        assertThat(String.join(" ", playbook.guidanceFor("quận 7", false))).contains("quá ngắn");
    }
}
