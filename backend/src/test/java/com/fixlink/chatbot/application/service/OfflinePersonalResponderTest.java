package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort.ToolExecutionResult;
import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OfflinePersonalResponderTest {

    private ChatbotToolPort toolPort;
    private OfflinePersonalResponder responder;

    @BeforeEach
    void setUp() {
        toolPort = mock(ChatbotToolPort.class);
        responder = new OfflinePersonalResponder(toolPort, new ObjectMapper());
    }

    @Test
    @DisplayName("AI quá tải: hỏi mã đơn cụ thể vẫn trả về trạng thái và bước tiếp theo thật")
    void shouldAnswerRequestStatusFromTool() {
        when(toolPort.executeTool(eq("get_repair_request_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("""
                        {"requestCode":"REQ-DEMO-0001","title":"Sửa máy lạnh","status":"ASSIGNED","statusLabel":"Đã chọn thợ",
                         "agreedPriceText":"400.000 đ","depositText":"120.000 đ","technicianName":"Lê Minh",
                         "nextStep":"Bấm Xác nhận lịch hẹn rồi quét QR đặt cọc."}
                        """, false));

        Optional<ChatOrchestrationResult> result = responder.answer("đơn REQ-DEMO-0001 đang ở trạng thái nào?", 7L, "CUSTOMER");

        assertThat(result).isPresent();
        assertThat(result.get().modelUsed()).isEqualTo("local-data");
        assertThat(result.get().reply()).contains("REQ-DEMO-0001").contains("Đã chọn thợ").contains("120.000 đ")
                .contains("Xác nhận lịch hẹn");
    }

    @Test
    @DisplayName("Hỏi đổi thợ / hủy cho một đơn cụ thể thì trả lời thẳng được hay không theo trạng thái thật")
    void shouldAnswerSwitchAndCancelByStatus() {
        when(toolPort.executeTool(eq("get_repair_request_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("{\"requestCode\":\"REQ-DEMO-0008\",\"status\":\"INSPECTING\",\"statusLabel\":\"Thợ đang khảo sát\"}", false))
                .thenReturn(new ToolExecutionResult("{\"requestCode\":\"REQ-DEMO-0002\",\"status\":\"IN_PROGRESS\",\"statusLabel\":\"Đang sửa chữa\"}", false));

        assertThat(responder.answer("đơn REQ-DEMO-0008 đổi thợ được không?", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("**Được đổi thợ.**");
        assertThat(responder.answer("hủy đơn REQ-DEMO-0002 được không", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("**Không hủy được**");
    }

    @Test
    @DisplayName("Hỏi tiền cọc của đơn chưa ghi nhận cọc thì tính 30% giá chốt")
    void shouldComputeDepositFromAgreedPrice() {
        when(toolPort.executeTool(eq("get_repair_request_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult(
                        "{\"requestCode\":\"REQ-DEMO-0001\",\"status\":\"ASSIGNED\",\"statusLabel\":\"Đã chọn thợ\",\"agreedPriceText\":\"350.000 đ\"}",
                        false));

        assertThat(responder.answer("tiền cọc đơn REQ-DEMO-0001 là bao nhiêu?", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("Tiền cọc dự kiến của đơn này là **105.000 đ**");
    }

    @Test
    @DisplayName("Hỏi danh sách đơn của tôi liệt kê mã đơn và trạng thái")
    void shouldListMyRequests() {
        when(toolPort.executeTool(eq("get_my_repair_requests"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("""
                        {"totalItems":2,"countsByGroup":{"Chờ thợ nhận":1,"Đang thực hiện":1,"Hoàn thành":0,"Đã hủy":0},"items":[
                          {"requestCode":"REQ-DEMO-0011","title":"Sửa tủ lạnh","statusLabel":"Chờ khách nghiệm thu"},
                          {"requestCode":"REQ-DEMO-0001","title":"Sửa máy lạnh","statusLabel":"Đã chọn thợ"}]}
                        """, false));

        String reply = responder.answer("đơn của tôi đang thế nào rồi?", 7L, "CUSTOMER").orElseThrow().reply();

        assertThat(reply).contains("tổng cộng **2** yêu cầu").contains("- Đang thực hiện: **1**")
                .contains("REQ-DEMO-0011").contains("Chờ khách nghiệm thu");
        assertThat(OfflinePersonalResponder.asksRequestOverview("hiện tại tôi đã có bao nhiêu yêu cầu sửa chữa")).isTrue();
        assertThat(OfflinePersonalResponder.asksRequestOverview("đơn REQ-DEMO-0001 của tôi sao rồi")).isFalse();
    }

    @Test
    @DisplayName("Thợ hỏi hồ sơ đã duyệt chưa thì đọc trạng thái KYC thật")
    void shouldAnswerTechnicianProfile() {
        when(toolPort.executeTool(eq("get_my_technician_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("""
                        {"fullName":"Lê Minh","verificationStatus":"APPROVED","verificationLabel":"Đã duyệt","completedJobs":68,"avgRating":4.92}
                        """, false));

        String reply = responder.answer("hồ sơ của tôi đã được duyệt chưa?", 9L, "TECHNICIAN").orElseThrow().reply();

        assertThat(reply).contains("Đã duyệt").contains("68");
    }

    @Test
    @DisplayName("Nhận ra câu hỏi về dữ liệu riêng và câu trả lời chỉ hứa 'đang kiểm tra' của AI")
    void shouldDetectPersonalQuestionsAndEmptyPromises() {
        assertThat(OfflinePersonalResponder.asksPersonalData("hiện tại tôi đã có bao nhiêu yêu cầu sửa chữa")).isTrue();
        assertThat(OfflinePersonalResponder.asksPersonalData("đơn REQ-DEMO-0001 sao rồi")).isTrue();
        assertThat(OfflinePersonalResponder.asksPersonalData("lịch hẹn của tôi khi nào")).isTrue();
        assertThat(OfflinePersonalResponder.asksPersonalData("cách đăng yêu cầu sửa chữa")).isFalse();
        assertThat(OfflinePersonalResponder.asksPersonalData("dời lịch hẹn thế nào")).isFalse();

        assertThat(OfflinePersonalResponder.isEmptyPromise("Mình đang kiểm tra danh sách yêu cầu sửa chữa của bạn trên hệ thống nhé."))
                .isTrue();
        assertThat(OfflinePersonalResponder.isEmptyPromise("Bạn có **3** yêu cầu, đơn REQ-DEMO-0001 đang chờ xác nhận lịch hẹn."))
                .isFalse();

        ChatOrchestrationResult login = OfflinePersonalResponder.loginRequired();
        assertThat(login.reply()).contains("đăng nhập");
        assertThat(login.actions()).extracting(a -> a.get("url")).contains("/dang-nhap");
    }

    @Test
    @DisplayName("Khách vãng lai hoặc câu không hỏi dữ liệu cá nhân thì không gọi tool")
    void shouldSkipWhenNotPersonal() {
        assertThat(responder.answer("đơn của tôi đang thế nào?", null, "GUEST")).isEmpty();
        assertThat(responder.answer("tiền cọc tính thế nào?", 7L, "CUSTOMER")).isEmpty();
        verify(toolPort, never()).executeTool(any(), anyMap(), any(), any(), any());
    }
}
