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

    @org.junit.jupiter.api.BeforeEach
    void resetLiveCatalog() {
        com.fixlink.chatbot.domain.service.LiveCatalog.reset();
    }

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
    @DisplayName("Quản trị viên hỏi số thợ đang hoạt động, hồ sơ chờ duyệt: trả lời bằng số liệu thật, đúng trọng tâm")
    void shouldAnswerAdminStatistics() {
        when(toolPort.executeTool(eq("get_admin_overview"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("""
                        {"technicians":{"total":5,"active":3,"pendingApproval":1,"pendingKyc":1,"blocked":0},
                         "customers":{"total":7,"active":7,"blocked":0},"allUsers":13,"pendingApprovalAll":1,"pendingKycAll":1,
                         "thisMonth":{"month":"2026-10","jobsCreated":29,"jobsAccepted":10,"completedJobs":4,"cancelledJobs":2,
                         "quotesCreated":12,"activeTechnicians":3,"completedJobValueText":"1.200.000 đ","platformRevenueText":"120.000 đ",
                         "averageRating":4.75,"reviewCount":4}}
                        """, false));

        assertThat(responder.answer("hiện tại có bao nhiêu thợ đang hoạt động", 1L, "ADMIN").orElseThrow().reply())
                .startsWith("Hiện có **3** thợ **đang hoạt động** (trên tổng **5** tài khoản thợ)");
        assertThat(responder.answer("có bao nhiêu hồ sơ chờ duyệt", 1L, "ADMIN").orElseThrow().reply())
                .contains("**1** tài khoản **chờ duyệt KYC**");
        assertThat(responder.answer("doanh thu tháng này", 1L, "ADMIN").orElseThrow().reply())
                .contains("120.000 đ").contains("tháng 10/2026");
        assertThat(responder.answer("duyệt hồ sơ thợ ở đâu", 1L, "ADMIN")).isEmpty();
    }

    @Test
    @DisplayName("Hỏi đã cọc chưa: trả lời theo trạng thái thật (Đã chọn thợ = chưa cọc, Đã đặt cọc trở đi = đã cọc)")
    void shouldTellWhetherDepositIsPaid() {
        when(toolPort.executeTool(eq("get_repair_request_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("{\"requestCode\":\"REQ-A\",\"status\":\"ASSIGNED\",\"depositText\":\"144.000 đ\"}", false))
                .thenReturn(new ToolExecutionResult("{\"requestCode\":\"REQ-B\",\"status\":\"INSPECTING\",\"depositText\":\"90.000 đ\"}", false));

        assertThat(responder.answer("đơn REQ-AAAA đã cọc chưa", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("Đơn này **chưa đặt cọc**").contains("144.000 đ");
        assertThat(responder.answer("đơn REQ-BBBB đã cọc chưa", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("Đơn này **đã đặt cọc**").contains("90.000 đ");
    }

    @Test
    @DisplayName("Thợ hỏi điểm đánh giá hoặc việc hôm nay: lấy hồ sơ thợ / lịch hẹn thật")
    void shouldAnswerTechnicianRatingAndToday() {
        when(toolPort.executeTool(eq("get_my_technician_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("{\"fullName\":\"Lê Minh\",\"verificationStatus\":\"APPROVED\","
                        + "\"verificationLabel\":\"Đã duyệt\",\"completedJobs\":12,\"avgRating\":\"4.8\","
                        + "\"walletBalanceText\":\"-45.000 đ\",\"walletNegative\":true}", false));
        when(toolPort.executeTool(eq("get_my_appointments"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult("{\"totalAppointments\":0,\"appointments\":[]}", false));

        assertThat(responder.answer("đánh giá trung bình của tôi bao nhiêu", 9L, "TECHNICIAN").orElseThrow().reply())
                .contains("4.8").contains("12");
        assertThat(responder.answer("số dư ví của tôi bao nhiêu", 9L, "TECHNICIAN").orElseThrow().reply())
                .contains("-45.000 đ").contains("tạm âm");
        assertThat(responder.answer("việc của tôi hôm nay", 9L, "TECHNICIAN")).isPresent();
        verify(toolPort).executeTool(eq("get_my_appointments"), anyMap(), any(), any(), any());
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
    @DisplayName("Hỏi phần còn lại phải thanh toán của một đơn thì tính giá chốt − tiền cọc")
    void shouldComputeRemainingPayment() {
        when(toolPort.executeTool(eq("get_repair_request_status"), anyMap(), any(), any(), any()))
                .thenReturn(new ToolExecutionResult(
                        "{\"requestCode\":\"REQ-DEMO-0011\",\"status\":\"AWAITING_ACCEPTANCE\",\"statusLabel\":\"Chờ nghiệm thu\","
                                + "\"agreedPriceText\":\"280.000 đ\",\"depositText\":\"84.000 đ\"}", false));

        assertThat(responder.answer("đơn REQ-DEMO-0011 thanh toán phần còn lại bao nhiêu", 7L, "CUSTOMER").orElseThrow().reply())
                .startsWith("Phần còn lại dự kiến của đơn này là **196.000 đ**");
    }

    @Test
    @DisplayName("Thợ xin lời khuyên nhận việc thì không bị liệt kê việc đang mở")
    void shouldNotListJobsWhenTechnicianAsksForAdvice() {
        assertThat(responder.answer("làm sao để nhận được nhiều việc hơn", 9L, "TECHNICIAN")).isEmpty();
        verify(toolPort, never()).executeTool(eq("find_matching_jobs"), anyMap(), any(), any(), any());
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
