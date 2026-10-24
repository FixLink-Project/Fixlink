package com.fixlink.chatbot.adapter.out.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
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
        ragIndex = mock(InMemoryRagIndex.class);
        objectMapper = new ObjectMapper();

        toolAdapter = new NativeChatbotToolAdapter(
                masterDataUseCase,
                repairRequestUseCase,
                technicianUseCase,
                appointmentUseCase,
                supportTicketUseCase,
                mock(QuotationUseCase.class),
                ragIndex,
                objectMapper
        );
    }

    @Test
    @DisplayName("Cung cấp đầy đủ định nghĩa 15 công cụ chuẩn Gemini API")
    void shouldProvideAllToolDeclarations() {
        List<Map<String, Object>> tools = toolAdapter.getToolDeclarations();
        assertThat(tools).hasSize(15);

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
