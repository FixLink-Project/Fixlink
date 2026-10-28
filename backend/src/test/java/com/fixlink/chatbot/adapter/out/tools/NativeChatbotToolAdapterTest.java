package com.fixlink.chatbot.adapter.out.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.application.port.in.AdminStatisticsUseCase;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.application.port.in.UserManagementUseCase;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.ServiceCategory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NativeChatbotToolAdapterTest {

    private MasterDataUseCase masterDataUseCase;
    private RepairRequestUseCase repairRequestUseCase;
    private TechnicianUseCase technicianUseCase;
    private AppointmentUseCase appointmentUseCase;
    private SupportTicketUseCase supportTicketUseCase;
    private UserManagementUseCase userManagementUseCase;
    private InMemoryRagIndex ragIndex;
    private ObjectMapper objectMapper;

    private NativeChatbotToolAdapter toolAdapter;

    @BeforeEach
    void setUp() {
        masterDataUseCase = mock(MasterDataUseCase.class);
        repairRequestUseCase = mock(RepairRequestUseCase.class);
        technicianUseCase = mock(TechnicianUseCase.class);
        appointmentUseCase = mock(AppointmentUseCase.class);
        supportTicketUseCase = mock(SupportTicketUseCase.class);
        userManagementUseCase = mock(UserManagementUseCase.class);
        ragIndex = mock(InMemoryRagIndex.class);
        objectMapper = new ObjectMapper();

        toolAdapter = new NativeChatbotToolAdapter(
                masterDataUseCase,
                repairRequestUseCase,
                technicianUseCase,
                appointmentUseCase,
                supportTicketUseCase,
                mock(QuotationUseCase.class),
                userManagementUseCase,
                mock(AdminStatisticsUseCase.class),
                ragIndex,
                objectMapper
        );
    }

    @Test
    @DisplayName("Đơn đã chọn thợ: bước tiếp theo khớp tình trạng lịch hẹn")
    void shouldMatchAssignedNextStepWithAppointment() {
        assertThat(NativeChatbotToolAdapter.assignedNextStep(Role.CUSTOMER, true, false))
                .contains("đã được xác nhận").contains("đặt cọc 30%").doesNotContain("Thợ sẽ đề xuất");
        assertThat(NativeChatbotToolAdapter.assignedNextStep(Role.CUSTOMER, false, true))
                .contains("Xác nhận lịch hẹn");
        assertThat(NativeChatbotToolAdapter.assignedNextStep(Role.CUSTOMER, false, false))
                .contains("Thợ sẽ đề xuất lịch hẹn");
        assertThat(NativeChatbotToolAdapter.assignedNextStep(Role.TECHNICIAN, true, false))
                .contains("Chờ khách thanh toán cọc");
    }

    @Test
    @DisplayName("Tổng quan quản trị: chỉ ADMIN xem được, đếm người dùng đúng theo vai trò và trạng thái")
    void shouldGiveAdminOverviewOnlyToAdmin() throws Exception {
        when(userManagementUseCase.getUsers(any())).thenAnswer(inv -> {
            UserManagementUseCase.UserQuery q = inv.getArgument(0);
            long total = switch (q.role() + "/" + q.status()) {
                case "TECHNICIAN/ALL" -> 5;
                case "TECHNICIAN/ACTIVE" -> 3;
                case "TECHNICIAN/PENDING" -> 1;
                case "CUSTOMER/ALL" -> 7;
                default -> 0;
            };
            return new UserManagementUseCase.UserPageResult(List.of(), 1, 1, total, 1);
        });

        ChatbotToolPort.ToolExecutionResult denied = toolAdapter.executeTool("get_admin_overview", Map.of(), 2L, "CUSTOMER", null);
        assertThat(denied.isError()).isTrue();

        ChatbotToolPort.ToolExecutionResult ok = toolAdapter.executeTool("get_admin_overview", Map.of(), 1L, "ADMIN", null);
        var node = objectMapper.readTree(ok.content());
        assertThat(node.path("technicians").path("active").asLong()).isEqualTo(3);
        assertThat(node.path("technicians").path("pendingKyc").asLong()).isEqualTo(1);
        assertThat(node.path("customers").path("total").asLong()).isEqualTo(7);
    }

    @Test
    @DisplayName("Cung cấp đầy đủ định nghĩa 16 công cụ chuẩn Gemini API")
    void shouldProvideAllToolDeclarations() {
        List<Map<String, Object>> tools = toolAdapter.getToolDeclarations();
        assertThat(tools).hasSize(16);

        List<String> toolNames = tools.stream()
                .map(t -> (String) t.get("name"))
                .toList();

        assertThat(toolNames).contains(
                "list_service_categories",
                "list_service_areas",
                "suggest_category",
                "get_my_repair_requests",
                "get_repair_request_status",
                "get_my_technician_status",
                "get_my_appointments",
                "create_support_ticket",
                "read_support_ticket",
                "search_knowledge_base",
                "list_services",
                "calculate_deposit",
                "find_matching_jobs",
                "prepare_repair_request",
                "get_request_quotations"
        );
    }

    @Test
    @DisplayName("Từ chối xem đơn sửa chữa nếu khách chưa đăng nhập")
    void shouldRequireLoginForPersonalRequests() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "get_my_repair_requests",
                Map.of(),
                null,
                "guest",
                null
        );

        assertThat(result.content()).contains("requireLogin").contains("Bạn cần đăng nhập");
    }

    @Test
    @DisplayName("Gợi ý danh mục hoạt động cho cả khách vãng lai")
    void shouldAllowSuggestCategoryForGuest() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "suggest_category",
                Map.of("symptom", "máy lạnh không lạnh"),
                null,
                "guest",
                null
        );

        assertThat(result.isError()).isFalse();
        assertThat(result.content()).contains("Điện Lạnh");
    }

    @Test
    @DisplayName("Máy lạnh chảy nước thuộc Điện lạnh và không bị coi là sự cố cháy nổ")
    void shouldClassifyLeakingAirConditionerWithoutDanger() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "suggest_category", Map.of("symptoms", "Máy lạnh chảy nước ở dàn lạnh"), null, "GUEST", null);

        assertThat(result.content()).contains("DIEN_LANH").contains("\"dangerDetected\":false");
    }

    @Test
    @DisplayName("Ổ cắm bốc khói được xếp Điện nước và bật cảnh báo an toàn")
    void shouldDetectDangerForSmokingSocket() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "suggest_category", Map.of("symptoms", "ổ cắm bị bốc khói, có mùi khét"), null, "GUEST", null);

        assertThat(result.content()).contains("DIEN_NUOC").contains("\"dangerDetected\":true").contains("114");
    }

    @Test
    @DisplayName("Tính cọc 30% làm tròn lên và phần còn lại 70%")
    void shouldCalculateDeposit() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "calculate_deposit", Map.of("amount", 500000), null, "GUEST", null);

        assertThat(result.isError()).isFalse();
        assertThat(result.content()).contains("150.000 đ").contains("350.000 đ");
    }

    @Test
    @DisplayName("Thợ chưa đăng nhập không xem được bảng việc phù hợp")
    void shouldRequireTechnicianLoginForMatchingJobs() {
        ChatbotToolPort.ToolExecutionResult result = toolAdapter.executeTool(
                "find_matching_jobs", Map.of(), 5L, "CUSTOMER", null);

        assertThat(result.content()).contains("requireLogin");
    }

    @Test
    @DisplayName("Chuẩn bị link form đăng yêu cầu điền sẵn, mã hóa tiếng Việt và bỏ khu vực ngoài danh sách")
    void shouldPreparePrefilledRequestLink() {
        ServiceCategory category = new ServiceCategory();
        category.setId(1L);
        category.setCode("DIEN_LANH");
        category.setName("Sửa Chữa Điện Lạnh");
        when(masterDataUseCase.getActiveCategories()).thenReturn(List.of(category));
        when(masterDataUseCase.getActiveAreas()).thenReturn(List.of(
                new MasterDataUseCase.ServiceAreaView(3L, "Q7", "Quận 7", "TP. Hồ Chí Minh")));

        ChatbotToolPort.ToolExecutionResult inArea = toolAdapter.executeTool("prepare_repair_request", Map.of(
                "category_code", "dien_lanh",
                "title", "Máy lạnh không mát",
                "description", "Máy lạnh Daikin chỉ thổi gió",
                "area_code", "q7",
                "budget", 300000), null, "GUEST", null);

        assertThat(inArea.isError()).isFalse();
        assertThat(inArea.content())
                .contains("/dang-yeu-cau?category=DIEN_LANH")
                .contains("M%C3%A1y%20l%E1%BA%A1nh")
                .contains("&area=Q7")
                .contains("&budget=300000")
                .contains("đăng nhập");

        ChatbotToolPort.ToolExecutionResult outOfArea = toolAdapter.executeTool("prepare_repair_request", Map.of(
                "category_code", "DIEN_LANH",
                "title", "Tủ lạnh không lạnh",
                "description", "Tủ lạnh hư",
                "area_code", "HA_NOI"), 2L, "CUSTOMER", null);

        assertThat(outOfArea.content()).doesNotContain("&area=").contains("khu vực (quận)");
    }
}
