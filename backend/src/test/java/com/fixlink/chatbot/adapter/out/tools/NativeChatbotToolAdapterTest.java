package com.fixlink.chatbot.adapter.out.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
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
                ragIndex,
                objectMapper
        );
    }

    @Test
    @DisplayName("Cung cấp đầy đủ định nghĩa 10 công cụ chuẩn Gemini API")
    void shouldProvideAllTenToolDeclarations() {
        List<Map<String, Object>> tools = toolAdapter.getToolDeclarations();
        assertThat(tools).hasSize(10);

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
                "search_knowledge_base"
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
}
