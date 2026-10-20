package com.fixlink.chatbot.adapter.out.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.response.AppointmentResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.ServiceCategory;
import com.fixlink.domain.model.TechnicianProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class NativeChatbotToolAdapter implements ChatbotToolPort {

    private static final Logger LOG = LoggerFactory.getLogger(NativeChatbotToolAdapter.class);

    private final MasterDataUseCase masterDataUseCase;
    private final RepairRequestUseCase repairRequestUseCase;
    private final TechnicianUseCase technicianUseCase;
    private final AppointmentUseCase appointmentUseCase;
    private final SupportTicketUseCase supportTicketUseCase;
    private final InMemoryRagIndex ragIndex;
    private final ObjectMapper objectMapper;

    public NativeChatbotToolAdapter(
            MasterDataUseCase masterDataUseCase,
            RepairRequestUseCase repairRequestUseCase,
            TechnicianUseCase technicianUseCase,
            AppointmentUseCase appointmentUseCase,
            SupportTicketUseCase supportTicketUseCase,
            InMemoryRagIndex ragIndex,
            ObjectMapper objectMapper
    ) {
        this.masterDataUseCase = masterDataUseCase;
        this.repairRequestUseCase = repairRequestUseCase;
        this.technicianUseCase = technicianUseCase;
        this.appointmentUseCase = appointmentUseCase;
        this.supportTicketUseCase = supportTicketUseCase;
        this.ragIndex = ragIndex;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> getToolDeclarations() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. list_service_categories
        tools.add(Map.of(
                "name", "list_service_categories",
                "description", "Lấy danh sách các danh mục dịch vụ sửa chữa đang hoạt động trên hệ thống FixLink (Điện lạnh, Điện nước, Gia dụng, Khóa & Cửa cuốn).",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of()
                )
        ));

        // 2. list_service_areas
        tools.add(Map.of(
                "name", "list_service_areas",
                "description", "Lấy danh sách các khu vực, quận huyện đang được phục vụ tại TP.HCM trên FixLink.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of()
                )
        ));

        // 3. suggest_category
        tools.add(Map.of(
                "name", "suggest_category",
                "description", "Gợi ý danh mục dịch vụ phù hợp dựa trên mô tả triệu chứng hư hỏng của thiết bị.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "symptoms", Map.of(
                                        "type", "STRING",
                                        "description", "Mô tả triệu chứng hoặc sự cố hỏng hóc của thiết bị"
                                )
                        ),
                        "required", List.of("symptoms")
                )
        ));

        // 4. get_my_repair_requests
        tools.add(Map.of(
                "name", "get_my_repair_requests",
                "description", "Lấy danh sách các yêu cầu sửa chữa của chính khách hàng đang đăng nhập.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "status", Map.of(
                                        "type", "STRING",
                                        "description", "Lọc theo trạng thái yêu cầu (ví dụ: OPEN, ASSIGNED, IN_PROGRESS, COMPLETED...)"
                                )
                        )
                )
        ));

        // 5. get_repair_request_status
        tools.add(Map.of(
                "name", "get_repair_request_status",
                "description", "Tra cứu trạng thái chi tiết và bước tiếp theo của một yêu cầu sửa chữa theo mã ID.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "request_id", Map.of(
                                        "type", "INTEGER",
                                        "description", "Mã số định danh của yêu cầu sửa chữa (requestId)"
                                )
                        ),
                        "required", List.of("request_id")
                )
        ));

        // 6. get_my_technician_status
        tools.add(Map.of(
                "name", "get_my_technician_status",
                "description", "Kiểm tra trạng thái hồ sơ của thợ: duyệt KYC (PENDING/APPROVED/REJECTED), online/offline, việc đang nhận và đánh giá sao.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of()
                )
        ));

        // 7. get_my_appointments
        tools.add(Map.of(
                "name", "get_my_appointments",
                "description", "Lấy danh sách lịch hẹn khảo sát / sửa chữa sắp tới của chính người dùng đang đăng nhập.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of()
                )
        ));

        // 8. create_support_ticket
        tools.add(Map.of(
                "name", "create_support_ticket",
                "description", "Tạo một phiếu hỗ trợ (ticket) mới khi cần chuyển tiếp nhân viên chăm sóc khách hàng, khiếu nại hoặc giải quyết sự cố.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "title", Map.of("type", "STRING", "description", "Tiêu đề ngắn gọn của vấn đề cần hỗ trợ"),
                                "description", Map.of("type", "STRING", "description", "Mô tả chi tiết vấn đề hoặc khiếu nại"),
                                "priority", Map.of("type", "STRING", "description", "Mức độ ưu tiên: Low, Medium, High hoặc Critical (mặc định: Medium)"),
                                "guest_name", Map.of("type", "STRING", "description", "Họ tên người cần hỗ trợ (bắt buộc nếu là khách vãng lai)"),
                                "guest_contact", Map.of("type", "STRING", "description", "Số điện thoại hoặc email liên hệ (bắt buộc nếu là khách vãng lai)")
                        ),
                        "required", List.of("title", "description")
                )
        ));

        // 9. read_support_ticket
        tools.add(Map.of(
                "name", "read_support_ticket",
                "description", "Đọc thông tin chi tiết một phiếu hỗ trợ theo mã TCK-XXXX hoặc xem danh sách phiếu hỗ trợ của chính mình.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "ticket_code", Map.of("type", "STRING", "description", "Mã phiếu hỗ trợ (ví dụ: TCK-1001)"),
                                "guest_contact", Map.of("type", "STRING", "description", "Số điện thoại hoặc email (dành cho khách vãng lai)")
                        )
                )
        ));

        // 10. search_knowledge_base
        tools.add(Map.of(
                "name", "search_knowledge_base",
                "description", "Tìm kiếm thông tin chính thức trong kho tri thức của FixLink về quy trình, chính sách, tài khoản, hướng dẫn sử dụng.",
                "parameters", Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "query", Map.of("type", "STRING", "description", "Từ khóa hoặc câu hỏi cần tìm kiếm trong kho tri thức")
                        ),
                        "required", List.of("query")
                )
        ));

        return tools;
    }

    @Override
    public ToolExecutionResult executeTool(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        try {
            LOG.info("Executing native tool '{}' with args {} for userId={}, role={}", toolName, arguments, currentUserId, currentRole);
            return switch (toolName) {
                case "list_service_categories" -> handleListCategories();
                case "list_service_areas" -> handleListAreas();
                case "suggest_category" -> handleSuggestCategory(arguments);
                case "get_my_repair_requests" -> handleGetMyRepairRequests(arguments, currentUserId, currentRole);
                case "get_repair_request_status" -> handleGetRepairRequestStatus(arguments, currentUserId, currentRole);
                case "get_my_technician_status" -> handleGetMyTechnicianStatus(currentUserId, currentRole);
                case "get_my_appointments" -> handleGetMyAppointments(currentUserId, currentRole);
                case "create_support_ticket" -> handleCreateSupportTicket(arguments, currentUserId, guestContact);
                case "read_support_ticket" -> handleReadSupportTicket(arguments, currentUserId, currentRole, guestContact);
                case "search_knowledge_base" -> handleSearchKnowledgeBase(arguments, currentRole);
                default -> new ToolExecutionResult(toJson(Map.of("isError", true, "message", "Không tìm thấy công cụ: " + toolName)), true);
            };
        } catch (Exception ex) {
            LOG.error("Error executing tool '{}'", toolName, ex);
            return new ToolExecutionResult(toJson(Map.of("isError", true, "message", "Lỗi xử lý công cụ: " + ex.getMessage())), true);
        }
    }

    private ToolExecutionResult handleListCategories() {
        List<ServiceCategory> categories = masterDataUseCase.getActiveCategories();
        List<Map<String, Object>> result = categories.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "code", c.getCode(),
                        "name", c.getName(),
                        "description", c.getDescription() != null ? c.getDescription() : ""
                ))
                .toList();
        return new ToolExecutionResult(toJson(Map.of("categories", result)), false);
    }

    private ToolExecutionResult handleListAreas() {
        var areas = masterDataUseCase.getActiveAreas();
        List<Map<String, Object>> result = areas.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.id(),
                        "code", a.code(),
                        "name", a.name(),
                        "city", a.city()
                ))
                .toList();
        return new ToolExecutionResult(toJson(Map.of("areas", result)), false);
    }

    private ToolExecutionResult handleSuggestCategory(Map<String, Object> arguments) {
        String symptoms = arguments.getOrDefault("symptoms", arguments.getOrDefault("symptom", "")).toString().toLowerCase(Locale.ROOT);
        String code = "DO_GIA_DUNG";
        String name = "Sửa Điện Gia Dụng";
        String reason = "Phù hợp với các thiết bị điện tử, điện gia dụng trong gia đình.";
        double confidence = 0.85;

        if (symptoms.contains("tủ lạnh") || symptoms.contains("máy lạnh") || symptoms.contains("điều hòa")
                || symptoms.contains("máy giặt") || symptoms.contains("tủ đông") || symptoms.contains("ga")
                || symptoms.contains("đóng tuyết") || symptoms.contains("chảy nước dàn lạnh")) {
            code = "DIEN_LANH";
            name = "Sửa Chữa Điện Lạnh";
            reason = "Các thiết bị nhiệt lạnh, làm lạnh và giặt sấy thuộc chuyên ngành Điện lạnh.";
            confidence = 0.95;
        } else if (symptoms.contains("nước") || symptoms.contains("vòi") || symptoms.contains("ống")
                || symptoms.contains("bồn cầu") || symptoms.contains("lavabo") || symptoms.contains("sen")
                || symptoms.contains("chập điện") || symptoms.contains("nhảy cb") || symptoms.contains("aptomat")
                || symptoms.contains("mất điện") || symptoms.contains("ổ cắm") || symptoms.contains("rò điện")) {
            code = "DIEN_NUOC";
            name = "Sửa Chữa Điện Nước";
            reason = "Sự cố liên quan đến hệ thống cấp thoát nước, đường dây điện sinh hoạt âm tường.";
            confidence = 0.95;
        } else if (symptoms.contains("khóa") || symptoms.contains("chìa") || symptoms.contains("cửa cuốn")
                || symptoms.contains("kẹt nan") || symptoms.contains("remote")) {
            code = "KHOA_CUA";
            name = "Sửa Khóa & Cửa Cuốn";
            reason = "Sự cố kẹt khóa, mất chìa hoặc cửa cuốn gia đình.";
            confidence = 0.95;
        }

        return new ToolExecutionResult(toJson(Map.of(
                "suggestedCategoryCode", code,
                "suggestedCategoryName", name,
                "confidence", confidence,
                "reason", reason
        )), false);
    }

    private ToolExecutionResult handleGetMyRepairRequests(Map<String, Object> arguments, Long currentUserId, String currentRole) {
        if (currentUserId == null || !"CUSTOMER".equalsIgnoreCase(currentRole)) {
            return new ToolExecutionResult(toJson(Map.of(
                    "requireLogin", true,
                    "message", "Bạn cần đăng nhập bằng tài khoản Khách hàng để xem danh sách yêu cầu của mình. Vui lòng đăng nhập tại /dang-nhap."
            )), false);
        }

        String status = arguments.containsKey("status") ? arguments.get("status").toString() : null;
        Page<RepairRequestResponse> page = repairRequestUseCase.getMyRequests(currentUserId, status, 0, 5, "createdAt", "DESC");

        List<Map<String, Object>> items = page.getContent().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "title", r.getTitle() != null ? r.getTitle() : "",
                        "categoryName", r.getCategoryName() != null ? r.getCategoryName() : "",
                        "status", r.getStatus() != null ? r.getStatus() : "",
                        "budget", r.getBudgetRef() != null ? r.getBudgetRef().toString() : (r.getAgreedPrice() != null ? r.getAgreedPrice().toString() : "0"),
                        "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
                ))
                .toList();

        return new ToolExecutionResult(toJson(Map.of(
                "totalItems", page.getTotalElements(),
                "items", items
        )), false);
    }

    private ToolExecutionResult handleGetRepairRequestStatus(Map<String, Object> arguments, Long currentUserId, String currentRole) {
        Object idObj = arguments.get("request_id");
        if (idObj == null) {
            return new ToolExecutionResult(toJson(Map.of("isError", true, "message", "Thiếu tham số request_id")), true);
        }

        Long requestId;
        try {
            requestId = Long.valueOf(idObj.toString());
        } catch (Exception e) {
            return new ToolExecutionResult(toJson(Map.of("isError", true, "message", "request_id không hợp lệ")), true);
        }

        if (currentUserId == null) {
            return new ToolExecutionResult(toJson(Map.of(
                    "requireLogin", true,
                    "message", "Bạn cần đăng nhập để xem trạng thái chi tiết của yêu cầu sửa chữa."
            )), false);
        }

        try {
            Role role = Role.valueOf(currentRole != null ? currentRole.toUpperCase() : "CUSTOMER");
            var detail = repairRequestUseCase.getRequestDetail(requestId, currentUserId, role);
            if (detail == null || detail.request() == null) {
                return new ToolExecutionResult(toJson(Map.of(
                        "isError", true,
                        "message", "Không tìm thấy yêu cầu sửa chữa hoặc bạn không có quyền xem thông tin yêu cầu này."
                )), true);
            }

            var req = detail.request();
            String nextStep = switch (req.getStatus()) {
                case DRAFT -> "Bấm 'Đăng yêu cầu' để mở cho các thợ nhận việc.";
                case OPEN -> "Yêu cầu đang mở. Hệ thống đang chờ thợ phù hợp bấm nhận việc.";
                case ASSIGNED -> "Thợ đã nhận việc. Hai bên thống nhất lịch hẹn để thợ đến khảo sát.";
                case INSPECTING -> "Thợ đang khảo sát hiện trường và chuẩn bị phương án sửa chữa.";
                case AWAITING_COST_APPROVAL -> "Thợ báo phát sinh chi phí. Khách hàng vui lòng duyệt chi phí mới.";
                case IN_PROGRESS -> "Thợ đang tiến hành sửa chữa thiết bị.";
                case AWAITING_ACCEPTANCE -> "Thợ đã sửa xong. Khách hàng kiểm tra thiết bị và bấm Nghiệm thu.";
                case COMPLETED -> "Yêu cầu đã hoàn tất. Bạn có thể đánh giá dịch vụ.";
                case CANCELLED -> "Yêu cầu đã bị hủy.";
                default -> "Theo dõi tiến độ trên hệ thống.";
            };

            return new ToolExecutionResult(toJson(Map.of(
                    "requestId", req.getId(),
                    "title", req.getTitle(),
                    "status", req.getStatus().name(),
                    "category", detail.categoryName() != null ? detail.categoryName() : "",
                    "address", req.getAddress() != null ? req.getAddress() : "",
                    "nextStep", nextStep
            )), false);
        } catch (Exception ex) {
            LOG.warn("Access denied or not found for requestId={}", requestId);
            return new ToolExecutionResult(toJson(Map.of(
                    "isError", true,
                    "message", "Không tìm thấy yêu cầu sửa chữa hoặc bạn không có quyền xem thông tin yêu cầu này."
            )), true);
        }
    }

    private ToolExecutionResult handleGetMyTechnicianStatus(Long currentUserId, String currentRole) {
        if (currentUserId == null || !"TECHNICIAN".equalsIgnoreCase(currentRole)) {
            return new ToolExecutionResult(toJson(Map.of(
                    "requireLogin", true,
                    "message", "Bạn cần đăng nhập bằng tài khoản Kỹ thuật viên (Thợ) để kiểm tra trạng thái hồ sơ."
            )), false);
        }

        TechnicianProfile profile = technicianUseCase.getMyProfile(currentUserId);
        if (profile == null) {
            return new ToolExecutionResult(toJson(Map.of("message", "Chưa tìm thấy hồ sơ kỹ thuật viên.")), false);
        }

        return new ToolExecutionResult(toJson(Map.of(
                "fullName", profile.getFullName() != null ? profile.getFullName() : "",
                "verificationStatus", profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : "PENDING",
                "isOnline", profile.getIsOnline() != null && profile.getIsOnline(),
                "completedJobs", profile.getCompletedJobs() != null ? profile.getCompletedJobs() : 0,
                "avgRating", profile.getAvgRating() != null ? profile.getAvgRating().toString() : "5.0",
                "rejectionReason", profile.getRejectionReason() != null ? profile.getRejectionReason() : ""
        )), false);
    }

    private ToolExecutionResult handleGetMyAppointments(Long currentUserId, String currentRole) {
        if (currentUserId == null) {
            return new ToolExecutionResult(toJson(Map.of(
                    "requireLogin", true,
                    "message", "Bạn cần đăng nhập để xem danh sách lịch hẹn của mình."
            )), false);
        }

        List<AppointmentResponse> list = appointmentUseCase.getMyAppointments(currentUserId, currentRole);
        List<Map<String, Object>> items = list.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "repairRequestId", a.getRepairRequestId(),
                        "scheduledDate", a.getScheduledDate() != null ? a.getScheduledDate().toString() : "",
                        "scheduledTime", a.getScheduledTime() != null ? a.getScheduledTime().toString() : "",
                        "status", a.getStatus() != null ? a.getStatus() : "",
                        "appointmentType", a.getAppointmentType() != null ? a.getAppointmentType() : ""
                ))
                .toList();

        return new ToolExecutionResult(toJson(Map.of(
                "totalAppointments", items.size(),
                "appointments", items
        )), false);
    }

    private ToolExecutionResult handleCreateSupportTicket(Map<String, Object> arguments, Long currentUserId, String guestContact) {
        String title = arguments.getOrDefault("title", "").toString();
        String description = arguments.getOrDefault("description", "").toString();
        String priorityStr = arguments.getOrDefault("priority", "Medium").toString();
        String guestName = arguments.containsKey("guest_name") ? arguments.get("guest_name").toString() : null;
        String contact = arguments.containsKey("guest_contact") ? arguments.get("guest_contact").toString() : guestContact;

        if (title.isBlank() || description.isBlank()) {
            return new ToolExecutionResult(toJson(Map.of("isError", true, "message", "Tiêu đề và nội dung mô tả ticket không được để trống")), true);
        }

        if (currentUserId == null && (contact == null || contact.isBlank())) {
            return new ToolExecutionResult(toJson(Map.of(
                    "requireContact", true,
                    "message", "Khách vãng lai vui lòng cung cấp Số điện thoại hoặc Email để chúng tôi có thể liên hệ hỗ trợ bạn."
            )), false);
        }

        TicketPriority priority = TicketPriority.fromString(priorityStr);
        SupportTicket ticket = supportTicketUseCase.createTicket(
                currentUserId,
                guestName,
                contact,
                title,
                description,
                priority,
                "Chưa phân công"
        );

        return new ToolExecutionResult(toJson(Map.of(
                "success", true,
                "ticketCode", ticket.getTicketCode(),
                "title", ticket.getTitle(),
                "status", ticket.getStatus().getValue(),
                "priority", ticket.getPriority().getValue(),
                "message", "Tạo phiếu hỗ trợ thành công. Mã phiếu của bạn là " + ticket.getTicketCode() + ". Nhân viên CSKH sẽ liên hệ với bạn trong thời gian sớm nhất."
        )), false);
    }

    private ToolExecutionResult handleReadSupportTicket(
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        String ticketCode = arguments.containsKey("ticket_code") ? arguments.get("ticket_code").toString() : null;
        String contact = arguments.containsKey("guest_contact") ? arguments.get("guest_contact").toString() : guestContact;
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentRole);

        if (ticketCode != null && !ticketCode.isBlank()) {
            Optional<SupportTicket> opt = supportTicketUseCase.getTicketByCode(ticketCode, currentUserId, isAdmin, contact);
            if (opt.isEmpty()) {
                return new ToolExecutionResult(toJson(Map.of(
                        "isError", true,
                        "message", "Không tìm thấy phiếu hỗ trợ hoặc bạn không có quyền xem thông tin phiếu này."
                )), true);
            }
            SupportTicket t = opt.get();
            return new ToolExecutionResult(toJson(Map.of(
                    "ticketCode", t.getTicketCode(),
                    "title", t.getTitle(),
                    "description", t.getDescription(),
                    "status", t.getStatus().getValue(),
                    "priority", t.getPriority().getValue(),
                    "assignee", t.getAssignee(),
                    "createdAt", t.getCreatedAt().toString()
            )), false);
        } else {
            if (currentUserId == null) {
                return new ToolExecutionResult(toJson(Map.of(
                        "requireLogin", true,
                        "message", "Bạn cần đăng nhập để xem danh sách các phiếu hỗ trợ của mình."
                )), false);
            }
            List<SupportTicket> myTickets = supportTicketUseCase.getMyTickets(currentUserId);
            List<Map<String, Object>> items = myTickets.stream()
                    .map(t -> Map.<String, Object>of(
                            "ticketCode", t.getTicketCode(),
                            "title", t.getTitle(),
                            "status", t.getStatus().getValue(),
                            "priority", t.getPriority().getValue(),
                            "createdAt", t.getCreatedAt().toString()
                    ))
                    .toList();
            return new ToolExecutionResult(toJson(Map.of(
                    "totalTickets", items.size(),
                    "tickets", items
            )), false);
        }
    }

    private ToolExecutionResult handleSearchKnowledgeBase(Map<String, Object> arguments, String currentRole) {
        String query = arguments.getOrDefault("query", "").toString();
        if (query.isBlank()) {
            return new ToolExecutionResult(toJson(Map.of("content", "")), false);
        }
        var res = ragIndex.retrieve(query, currentRole);
        return new ToolExecutionResult(toJson(Map.of(
                "content", res.mergedContent(),
                "sources", res.sources()
        )), false);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
