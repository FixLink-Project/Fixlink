package com.fixlink.chatbot.adapter.out.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.response.AppointmentResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestDetail;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestPageResult;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestQuery;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.chatbot.domain.service.RepairRequestDraftLink;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import com.fixlink.domain.model.RepairRequest;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.RequestTab;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import com.fixlink.domain.model.TechnicianProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Bộ công cụ (function calling) chatbot gọi trực tiếp vào các use case nghiệp vụ của FixLink.
 *
 * <p>Nguyên tắc: tool chỉ ĐỌC dữ liệu của chính người đang đăng nhập (trừ tạo phiếu hỗ trợ),
 * không bao giờ thực hiện thao tác có hậu quả tài chính hay đổi trạng thái đơn hộ người dùng.
 */
@Component
public class NativeChatbotToolAdapter implements ChatbotToolPort {

    private static final Logger LOG = LoggerFactory.getLogger(NativeChatbotToolAdapter.class);

    /** Tỷ lệ cọc khi khách chọn thợ — khớp {@code QuotationService.DEPOSIT_RATE}. */
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");
    private static final Pattern REQUEST_CODE_PATTERN = Pattern.compile("REQ-\\d{8}-[A-Z0-9]{6}", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final int LIST_LIMIT = 10;

    private final MasterDataUseCase masterDataUseCase;
    private final RepairRequestUseCase repairRequestUseCase;
    private final TechnicianUseCase technicianUseCase;
    private final AppointmentUseCase appointmentUseCase;
    private final SupportTicketUseCase supportTicketUseCase;
    private final QuotationUseCase quotationUseCase;
    private final InMemoryRagIndex ragIndex;
    private final ObjectMapper objectMapper;

    public NativeChatbotToolAdapter(
            MasterDataUseCase masterDataUseCase,
            RepairRequestUseCase repairRequestUseCase,
            TechnicianUseCase technicianUseCase,
            AppointmentUseCase appointmentUseCase,
            SupportTicketUseCase supportTicketUseCase,
            QuotationUseCase quotationUseCase,
            InMemoryRagIndex ragIndex,
            ObjectMapper objectMapper
    ) {
        this.masterDataUseCase = masterDataUseCase;
        this.repairRequestUseCase = repairRequestUseCase;
        this.technicianUseCase = technicianUseCase;
        this.appointmentUseCase = appointmentUseCase;
        this.supportTicketUseCase = supportTicketUseCase;
        this.quotationUseCase = quotationUseCase;
        this.ragIndex = ragIndex;
        this.objectMapper = objectMapper;
    }

    // ==================================================================================
    // KHAI BÁO TOOL (Gemini functionDeclarations)
    // ==================================================================================

    @Override
    public List<Map<String, Object>> getToolDeclarations() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(tool("list_service_categories",
                "Lấy danh sách danh mục ngành nghề sửa chữa đang hoạt động trên FixLink (dữ liệu thật từ hệ thống).",
                Map.of(), List.of()));

        tools.add(tool("list_services",
                "Lấy các gói dịch vụ cụ thể kèm GIÁ THAM KHẢO khởi điểm (VND) và thời gian thực hiện ước tính. "
                        + "Dùng khi khách hỏi giá, chi phí, bao nhiêu tiền, hoặc FixLink có làm dịch vụ X không.",
                Map.of("category_code", prop("STRING",
                        "Mã danh mục để lọc: DIEN_LANH, DIEN_NUOC, DO_GIA_DUNG, KHOA_CUA. Bỏ trống để lấy tất cả.")),
                List.of()));

        tools.add(tool("list_service_areas",
                "Lấy danh sách khu vực (quận/huyện) FixLink đang phục vụ (dữ liệu thật từ hệ thống).",
                Map.of(), List.of()));

        tools.add(tool("suggest_category",
                "Gợi ý danh mục dịch vụ và gói dịch vụ phù hợp dựa trên mô tả triệu chứng hư hỏng của thiết bị, "
                        + "đồng thời phát hiện dấu hiệu nguy hiểm (cháy, chập, rò gas...).",
                Map.of("symptoms", prop("STRING", "Mô tả triệu chứng / sự cố của thiết bị, càng đầy đủ càng tốt")),
                List.of("symptoms")));

        tools.add(tool("calculate_deposit",
                "Tính chính xác tiền cọc (30%) và phần còn lại (70%) theo quy định FixLink cho một mức giá/ngân sách.",
                Map.of("amount", prop("NUMBER", "Mức giá hoặc ngân sách tính bằng VND, ví dụ 500000")),
                List.of("amount")));

        Map<String, Object> prepareProps = new LinkedHashMap<>();
        prepareProps.put("category_code", prop("STRING", "Mã danh mục: DIEN_LANH, DIEN_NUOC, DO_GIA_DUNG hoặc KHOA_CUA"));
        prepareProps.put("title", prop("STRING", "Tiêu đề ngắn gọn, ví dụ 'Máy lạnh Daikin chỉ thổi gió, không mát'"));
        prepareProps.put("description", prop("STRING", "Mô tả sự cố tổng hợp từ hội thoại: thiết bị, hãng/model, hiện tượng, thời điểm, khung giờ mong muốn"));
        prepareProps.put("area_code", prop("STRING", "Mã khu vực nếu khách đã nêu quận: Q1, Q3, Q7, BINH_THANH, THU_DUC, TAN_BINH, GO_VAP, PHU_NHUAN"));
        prepareProps.put("budget", prop("NUMBER", "Ngân sách VND nếu khách đã nêu"));
        tools.add(tool("prepare_repair_request",
                "Chuẩn bị nút 'Đăng yêu cầu với thông tin này' điền sẵn form đăng yêu cầu từ nội dung khách đã mô tả. "
                        + "KHÔNG tạo đơn — khách tự kiểm tra và bấm đăng. Gọi khi đã biết thiết bị và hiện tượng.",
                prepareProps, List.of("category_code", "title", "description")));

        tools.add(tool("get_my_repair_requests",
                "Lấy danh sách yêu cầu sửa chữa của người dùng đang đăng nhập: với KHÁCH HÀNG là các đơn họ đã đăng; "
                        + "với THỢ là các việc thợ đã nhận.",
                Map.of("status", prop("STRING",
                        "Lọc theo trạng thái (OPEN, ASSIGNED, DEPOSIT_PAID, INSPECTING, AWAITING_COST_APPROVAL, IN_PROGRESS, "
                                + "AWAITING_ACCEPTANCE, COMPLETED, CANCELLED, DRAFT) hoặc nhóm tab "
                                + "(AWAITING_QUOTE = đang nhận báo giá, IN_PROGRESS = đang thực hiện). Bỏ trống để lấy tất cả: kết quả có "
                                + "totalItems (tổng số đơn) và countsByGroup (số đơn theo từng nhóm), đủ để trả lời 'có bao nhiêu đơn' "
                                + "mà không cần gọi lại.")),
                List.of()));

        Map<String, Object> statusProps = new LinkedHashMap<>();
        statusProps.put("request_id", prop("INTEGER", "ID số của yêu cầu (ví dụ 15, nằm cuối đường dẫn /yeu-cau-cua-toi/15)"));
        statusProps.put("request_code", prop("STRING", "Mã yêu cầu dạng REQ-YYYYMMDD-XXXXXX"));
        tools.add(tool("get_repair_request_status",
                "Tra cứu chi tiết một yêu cầu sửa chữa của người dùng (trạng thái, giá chốt, tiền cọc, thợ phụ trách, "
                        + "lịch hẹn, bước tiếp theo). Cần request_id HOẶC request_code.",
                statusProps, List.of()));

        Map<String, Object> quoteProps = new LinkedHashMap<>();
        quoteProps.put("request_id", prop("INTEGER", "ID số của yêu cầu (số ở cuối đường dẫn /yeu-cau-cua-toi/{id} hoặc /tho/yeu-cau/{id})"));
        quoteProps.put("request_code", prop("STRING", "Mã yêu cầu dạng REQ-YYYYMMDD-XXXXXX"));
        tools.add(tool("get_request_quotations",
                "Xem các báo giá của một yêu cầu: với KHÁCH là danh sách thợ đã báo giá (tên, điểm đánh giá, số việc đã làm, "
                        + "giá công + vật tư, phương án, trạng thái) để so sánh; với THỢ là báo giá của mình và giá của đối thủ "
                        + "(ẩn danh). Chỉ để so sánh — không chọn thợ hay gửi báo giá thay người dùng.",
                quoteProps, List.of()));

        tools.add(tool("get_my_technician_status",
                "Kiểm tra hồ sơ thợ đang đăng nhập: trạng thái duyệt KYC (PENDING/APPROVED/REJECTED) và lý do từ chối, "
                        + "online/offline, số việc đã hoàn thành, điểm đánh giá.",
                Map.of(), List.of()));

        tools.add(tool("find_matching_jobs",
                "Dành cho THỢ đã đăng nhập: liệt kê các yêu cầu đang mở (OPEN, còn hạn) khớp chuyên môn và khu vực "
                        + "của thợ để thợ vào trang chi tiết gửi báo giá.",
                Map.of("keyword", prop("STRING", "Từ khóa lọc theo tiêu đề/mô tả (tùy chọn)")),
                List.of()));

        tools.add(tool("get_my_appointments",
                "Lấy danh sách lịch hẹn khảo sát / sửa chữa của người dùng đang đăng nhập (khách hàng hoặc thợ).",
                Map.of(), List.of()));

        Map<String, Object> ticketProps = new LinkedHashMap<>();
        ticketProps.put("title", prop("STRING", "Tiêu đề ngắn gọn của vấn đề"));
        ticketProps.put("description", prop("STRING", "Mô tả chi tiết vấn đề, kèm mã đơn nếu có"));
        ticketProps.put("priority", prop("STRING", "Low | Medium | High | Critical (mặc định Medium)"));
        ticketProps.put("guest_name", prop("STRING", "Họ tên người cần hỗ trợ (khách vãng lai)"));
        ticketProps.put("guest_contact", prop("STRING", "Số điện thoại hoặc email liên hệ (bắt buộc với khách vãng lai)"));
        tools.add(tool("create_support_ticket",
                "Tạo phiếu hỗ trợ (ticket) chuyển cho nhân viên CSKH. CHỈ gọi sau khi người dùng đã đồng ý rõ ràng "
                        + "việc tạo phiếu và đã có đủ thông tin (khách vãng lai phải cung cấp SĐT/email).",
                ticketProps, List.of("title", "description")));

        Map<String, Object> readTicketProps = new LinkedHashMap<>();
        readTicketProps.put("ticket_code", prop("STRING", "Mã phiếu hỗ trợ dạng TCK-XXXXXXXX. Bỏ trống để liệt kê phiếu của tôi"));
        readTicketProps.put("guest_contact", prop("STRING", "SĐT hoặc email đã dùng khi tạo phiếu (khách vãng lai)"));
        tools.add(tool("read_support_ticket",
                "Xem chi tiết một phiếu hỗ trợ theo mã TCK-XXXXXXXX, hoặc liệt kê các phiếu hỗ trợ của chính mình.",
                readTicketProps, List.of()));

        tools.add(tool("search_knowledge_base",
                "Tìm kiếm thêm trong kho tri thức FixLink (quy trình, chính sách, tài khoản, xử lý sự cố) khi cần trích dẫn nguồn.",
                Map.of("query", prop("STRING", "Câu hỏi hoặc từ khóa cần tìm")),
                List.of("query")));

        return tools;
    }

    private static Map<String, Object> tool(String name, String description,
                                            Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "OBJECT");
        parameters.put("properties", properties);
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }
        return Map.of("name", name, "description", description, "parameters", parameters);
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    // ==================================================================================
    // THỰC THI TOOL
    // ==================================================================================

    @Override
    public ToolExecutionResult executeTool(
            String toolName,
            Map<String, Object> arguments,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        Map<String, Object> args = arguments != null ? arguments : Map.of();
        try {
            LOG.info("Executing native tool '{}' for userId={}, role={}", toolName, currentUserId, currentRole);
            return switch (toolName) {
                case "list_service_categories" -> handleListCategories();
                case "list_services" -> handleListServices(args);
                case "list_service_areas" -> handleListAreas();
                case "suggest_category" -> handleSuggestCategory(args);
                case "calculate_deposit" -> handleCalculateDeposit(args);
                case "prepare_repair_request" -> handlePrepareRepairRequest(args, currentUserId, currentRole);
                case "get_my_repair_requests" -> handleGetMyRepairRequests(args, currentUserId, currentRole);
                case "get_repair_request_status" -> handleGetRepairRequestStatus(args, currentUserId, currentRole);
                case "get_my_technician_status" -> handleGetMyTechnicianStatus(currentUserId, currentRole);
                case "find_matching_jobs" -> handleFindMatchingJobs(args, currentUserId, currentRole);
                case "get_request_quotations" -> handleGetRequestQuotations(args, currentUserId, currentRole);
                case "get_my_appointments" -> handleGetMyAppointments(currentUserId, currentRole);
                case "create_support_ticket" -> handleCreateSupportTicket(args, currentUserId, guestContact);
                case "read_support_ticket" -> handleReadSupportTicket(args, currentUserId, currentRole, guestContact);
                case "search_knowledge_base" -> handleSearchKnowledgeBase(args, currentRole);
                default -> error("Không tìm thấy công cụ: " + toolName);
            };
        } catch (Exception ex) {
            LOG.error("Error executing tool '{}'", toolName, ex);
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return error("Không thể lấy dữ liệu lúc này: " + message);
        }
    }

    private ToolExecutionResult handleListCategories() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceCategory c : masterDataUseCase.getActiveCategories()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("code", c.getCode());
            item.put("name", c.getName());
            item.put("description", nvl(c.getDescription()));
            result.add(item);
        }
        return ok(Map.of("totalCategories", result.size(), "categories", result));
    }

    private ToolExecutionResult handleListServices(Map<String, Object> args) {
        List<ServiceCategory> categories = masterDataUseCase.getActiveCategories();
        Map<Long, ServiceCategory> categoryById = new LinkedHashMap<>();
        for (ServiceCategory c : categories) {
            categoryById.put(c.getId(), c);
        }

        String categoryCode = str(args.get("category_code"));
        Long categoryId = null;
        if (categoryCode != null) {
            String normalized = categoryCode.trim().toUpperCase(Locale.ROOT);
            categoryId = categories.stream()
                    .filter(c -> normalized.equalsIgnoreCase(c.getCode()) || normalized.equalsIgnoreCase(c.getName()))
                    .map(ServiceCategory::getId)
                    .findFirst()
                    .orElse(null);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Service s : masterDataUseCase.getServices(categoryId)) {
            if (Boolean.FALSE.equals(s.getIsActive())) {
                continue;
            }
            ServiceCategory category = categoryById.get(s.getCategoryId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", s.getCode());
            item.put("name", s.getName());
            item.put("description", nvl(s.getDescription()));
            item.put("categoryName", category != null ? category.getName() : "");
            item.put("basePriceVnd", s.getBasePrice());
            item.put("basePriceText", formatVnd(s.getBasePrice()));
            item.put("estimatedDurationMinutes", s.getEstimatedDurationMinutes());
            items.add(item);
        }
        return ok(Map.of(
                "totalServices", items.size(),
                "services", items,
                "note", "Giá là GIÁ THAM KHẢO khởi điểm của gói dịch vụ. Khách đặt ngân sách dự kiến, giá thực tế là báo giá của thợ mà khách chọn "
                        + "khi đăng yêu cầu; nếu thợ khảo sát phát hiện lỗi phát sinh thì thợ đề xuất chi phí mới để khách duyệt."
        ));
    }

    private ToolExecutionResult handleListAreas() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var a : masterDataUseCase.getActiveAreas()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", a.code());
            item.put("name", a.name());
            item.put("city", nvl(a.city()));
            result.add(item);
        }
        return ok(Map.of("totalAreas", result.size(), "areas", result));
    }

    private ToolExecutionResult handlePrepareRepairRequest(Map<String, Object> args, Long currentUserId, String currentRole) {
        String categoryCode = str(args.get("category_code"));
        String title = str(args.get("title"));
        String description = str(args.get("description"));
        if (categoryCode == null || title == null || description == null) {
            return error("Cần category_code, title và description để chuẩn bị form đăng yêu cầu");
        }

        ServiceCategory category = masterDataUseCase.getActiveCategories().stream()
                .filter(c -> categoryCode.trim().equalsIgnoreCase(c.getCode()))
                .findFirst()
                .orElse(null);
        if (category == null) {
            return error("Danh mục " + categoryCode + " không tồn tại hoặc đã ngừng hoạt động. "
                    + "Danh mục hợp lệ: DIEN_LANH, DIEN_NUOC, DO_GIA_DUNG, KHOA_CUA.");
        }

        String requestedArea = str(args.get("area_code"));
        String areaCode = requestedArea;
        String areaName = null;
        if (requestedArea != null) {
            var area = masterDataUseCase.getActiveAreas().stream()
                    .filter(a -> requestedArea.trim().equalsIgnoreCase(a.code()))
                    .findFirst()
                    .orElse(null);
            if (area == null) {
                areaCode = null; // khu vực ngoài danh sách phục vụ: không điền sẵn
            } else {
                areaCode = area.code();
                areaName = area.name();
            }
        }

        String cleanTitle = title.length() < 4 ? title + " - cần sửa" : truncate(title, 200);
        String cleanDescription = truncate(description, 1500);
        BigDecimal budget = null;
        Object budgetArg = args.get("budget");
        if (budgetArg != null) {
            try {
                budget = new BigDecimal(String.valueOf(budgetArg).replaceAll("[^0-9.]", ""));
                if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                    budget = null;
                }
            } catch (Exception ignored) {
                budget = null;
            }
        }

        String url = RepairRequestDraftLink.build(category.getCode(), cleanTitle, cleanDescription, areaCode,
                budget != null ? budget.setScale(0, RoundingMode.HALF_UP).longValue() : null);

        List<String> stillNeeded = new ArrayList<>(List.of("địa chỉ cụ thể", "ảnh/video hiện trường (khuyến khích, tối đa 6)"));
        if (areaCode == null) {
            stillNeeded.add(0, "khu vực (quận)");
        }
        if (budget == null) {
            stillNeeded.add("ngân sách dự kiến (bắt buộc, > 0)");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prefillUrl", url);
        result.put("categoryName", category.getName());
        result.put("title", cleanTitle);
        if (areaName != null) {
            result.put("areaName", areaName);
        }
        if (budget != null) {
            result.put("budgetText", formatVnd(budget));
        }
        result.put("stillNeeded", stillNeeded);
        boolean customerLoggedIn = currentUserId != null && parseRole(currentRole) == Role.CUSTOMER;
        result.put("note", customerLoggedIn
                ? "Hệ thống sẽ hiện nút mở form đã điền sẵn; khách kiểm tra, bổ sung phần còn thiếu rồi bấm Đăng yêu cầu."
                : "Hệ thống sẽ hiện nút mở form đã điền sẵn; khách cần đăng nhập tài khoản khách hàng trước, "
                        + "sau khi đăng nhập form vẫn giữ nguyên thông tin.");
        return ok(result);
    }

    private ToolExecutionResult handleCalculateDeposit(Map<String, Object> args) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(String.valueOf(args.get("amount")).replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return error("Số tiền không hợp lệ, vui lòng cung cấp số VND, ví dụ 500000");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return error("Số tiền phải lớn hơn 0");
        }
        BigDecimal deposit = amount.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.CEILING);
        BigDecimal remaining = amount.setScale(0, RoundingMode.HALF_UP).subtract(deposit);
        return ok(Map.of(
                "amountText", formatVnd(amount),
                "depositRate", "30%",
                "depositText", formatVnd(deposit),
                "remainingRate", "70%",
                "remainingText", formatVnd(remaining),
                "rule", "Tiền cọc = 30% giá đã chốt, khách trả qua mã QR chuyển khoản sau khi xác nhận lịch hẹn; FixLink giữ bảo chứng. "
                        + "Phần còn lại (70% nếu không có phát sinh) trả qua QR khi thợ báo hoàn thành = giá chốt + phát sinh đã duyệt − tiền cọc. "
                        + "Duyệt chi phí phát sinh KHÔNG làm thay đổi tiền cọc."
        ));
    }

    // ---------------------------------------------------------------- suggest_category

    private ToolExecutionResult handleSuggestCategory(Map<String, Object> args) {
        String raw = firstNonBlank(str(args.get("symptoms")), str(args.get("symptom")), str(args.get("description")));
        if (raw == null) {
            return error("Vui lòng mô tả triệu chứng hư hỏng của thiết bị");
        }

        Optional<SymptomClassifier.CategoryMatch> match = SymptomClassifier.classify(raw);
        boolean danger = SymptomClassifier.isDanger(raw);

        Map<String, Object> result = new LinkedHashMap<>();
        if (match.isEmpty()) {
            result.put("matched", false);
            result.put("message", "Chưa xác định chắc chắn danh mục. Hãy hỏi thêm khách: thiết bị gì, hiện tượng cụ thể ra sao.");
            result.put("availableCategories", SymptomClassifier.categoryNames());
        } else {
            SymptomClassifier.CategoryMatch best = match.get();
            result.put("matched", true);
            result.put("suggestedCategoryCode", best.code());
            result.put("suggestedCategoryName", best.name());
            result.put("confidence", best.confidence());
            result.put("reason", best.reason());
            result.put("relatedServices", relatedServices(best.code()));
        }
        result.put("dangerDetected", danger);
        if (danger) {
            result.put("safetyAdvice", "Có dấu hiệu nguy hiểm: ngắt cầu dao/aptomat tổng, khóa van gas, tránh xa khu vực, "
                    + "không tự sửa; khẩn cấp gọi 114 (cứu hỏa) hoặc 115 (cấp cứu). Sau khi an toàn mới đặt thợ.");
        }
        return ok(result);
    }

    private List<Map<String, Object>> relatedServices(String categoryCode) {
        try {
            Long categoryId = masterDataUseCase.getActiveCategories().stream()
                    .filter(c -> categoryCode.equalsIgnoreCase(c.getCode()))
                    .map(ServiceCategory::getId)
                    .findFirst()
                    .orElse(null);
            if (categoryId == null) {
                return List.of();
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (Service s : masterDataUseCase.getServices(categoryId)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", s.getName());
                item.put("basePriceText", formatVnd(s.getBasePrice()));
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---------------------------------------------------------------- yêu cầu sửa chữa

    private ToolExecutionResult handleGetMyRepairRequests(Map<String, Object> args, Long currentUserId, String currentRole) {
        Role role = parseRole(currentRole);
        if (currentUserId == null || (role != Role.CUSTOMER && role != Role.TECHNICIAN)) {
            return requireLogin("Bạn cần đăng nhập (tài khoản Khách hàng tại /dang-nhap hoặc Thợ tại /dang-nhap-tho) "
                    + "để xem danh sách yêu cầu của mình.");
        }

        String statusArg = str(args.get("status"));
        RequestStatus statusFilter = parseStatus(statusArg);
        String tab = statusFilter == null && statusArg != null ? RequestTab.fromCode(statusArg).name() : null;

        RepairRequestPageResult page = repairRequestUseCase.listRequests(new RepairRequestQuery(
                1, statusFilter != null ? 50 : LIST_LIMIT, statusFilter != null ? statusFilter.name() : tab,
                null, "createdAt", "DESC", currentUserId, role));

        List<Map<String, Object>> items = new ArrayList<>();
        for (RepairRequestDetail d : page.items()) {
            RepairRequest r = d.request();
            if (statusFilter != null && r.getStatus() != statusFilter) {
                continue;
            }
            if (items.size() >= LIST_LIMIT) {
                break;
            }
            items.add(summarize(d, role));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("viewerRole", role.name());
        result.put("totalItems", statusFilter != null ? items.size() : page.totalItems());
        if (statusArg == null || statusArg.isBlank()) {
            // Đếm sẵn theo nhóm như các tab trên trang "Yêu cầu của tôi" để trả lời "có bao nhiêu đơn" trong một lần gọi
            Map<String, Object> counts = new LinkedHashMap<>();
            for (RequestTab group : List.of(RequestTab.AWAITING_QUOTE, RequestTab.IN_PROGRESS,
                    RequestTab.COMPLETED, RequestTab.CANCELLED)) {
                long count = repairRequestUseCase.listRequests(new RepairRequestQuery(
                        1, 1, group.name(), null, "createdAt", "DESC", currentUserId, role)).totalItems();
                counts.put(group.getLabel(), count);
            }
            result.put("countsByGroup", counts);
        }
        result.put("shownItems", items.size());
        result.put("items", items);
        if (items.isEmpty()) {
            result.put("message", role == Role.CUSTOMER
                    ? "Bạn chưa có yêu cầu sửa chữa nào" + (statusArg != null ? " ở trạng thái này" : "")
                            + ". Có thể đăng yêu cầu mới tại /dang-yeu-cau."
                    : "Bạn chưa được khách chọn cho việc nào" + (statusArg != null ? " ở trạng thái này" : "")
                            + ". Xem việc phù hợp tại bảng việc /tho.");
        }
        return ok(result);
    }

    private ToolExecutionResult handleGetRepairRequestStatus(Map<String, Object> args, Long currentUserId, String currentRole) {
        if (currentUserId == null) {
            return requireLogin("Bạn cần đăng nhập để xem chi tiết yêu cầu sửa chữa của mình.");
        }
        Role role = parseRole(currentRole);
        if (role == null) {
            return requireLogin("Bạn cần đăng nhập để xem chi tiết yêu cầu sửa chữa của mình.");
        }

        Long requestId = parseLong(args.get("request_id"));
        String requestCode = str(args.get("request_code"));
        if (requestId == null && requestCode == null) {
            return error("Cần cung cấp ID yêu cầu (số) hoặc mã yêu cầu dạng REQ-YYYYMMDD-XXXXXX");
        }
        if (requestId == null) {
            Matcher m = REQUEST_CODE_PATTERN.matcher(requestCode);
            String code = m.find() ? m.group().toUpperCase(Locale.ROOT) : requestCode.trim().toUpperCase(Locale.ROOT);
            requestId = findRequestIdByCode(code, currentUserId, role);
            if (requestId == null) {
                return error("Không tìm thấy yêu cầu có mã " + code + " trong danh sách yêu cầu của bạn.");
            }
        }

        RepairRequestDetail detail;
        try {
            detail = repairRequestUseCase.getRequestDetail(requestId, currentUserId, role);
        } catch (Exception ex) {
            LOG.info("Chatbot denied/not found requestId={} for userId={}: {}", requestId, currentUserId, ex.getMessage());
            return error("Không tìm thấy yêu cầu sửa chữa này hoặc bạn không có quyền xem (chỉ chủ đơn hoặc thợ phụ trách mới xem được).");
        }
        if (detail == null || detail.request() == null) {
            return error("Không tìm thấy yêu cầu sửa chữa này hoặc bạn không có quyền xem.");
        }

        Map<String, Object> result = summarize(detail, role);
        RepairRequest req = detail.request();
        result.put("address", nvl(req.getAddress()));
        if (req.getCancelReason() != null) {
            result.put("cancelReason", req.getCancelReason());
        }
        result.put("canCustomerCancel", role == Role.CUSTOMER && isCancellableByCustomer(req.getStatus()));
        result.put("nextStep", nextStep(req.getStatus(), role));
        result.put("detailUrl", (role == Role.TECHNICIAN ? "/tho/yeu-cau/" : "/yeu-cau-cua-toi/") + req.getId());

        try {
            List<Map<String, Object>> appointments = new ArrayList<>();
            for (AppointmentResponse a : appointmentUseCase.getAppointmentsForRequest(req.getId(), currentUserId, role.name())) {
                appointments.add(summarizeAppointment(a));
            }
            result.put("appointments", appointments);
        } catch (Exception ignored) {
            // không có quyền / chưa có lịch hẹn — bỏ qua
        }
        return ok(result);
    }

    private Long findRequestIdByCode(String code, Long userId, Role role) {
        RepairRequestPageResult page = repairRequestUseCase.listRequests(new RepairRequestQuery(
                1, 100, null, code, "createdAt", "DESC", userId, role));
        return page.items().stream()
                .map(RepairRequestDetail::request)
                .filter(r -> code.equalsIgnoreCase(r.getRequestCode()))
                .map(RepairRequest::getId)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> summarize(RepairRequestDetail d, Role viewerRole) {
        RepairRequest r = d.request();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", r.getId());
        item.put("requestCode", nvl(r.getRequestCode()));
        item.put("title", nvl(r.getTitle()));
        item.put("categoryName", nvl(d.categoryName()));
        item.put("status", r.getStatus() != null ? r.getStatus().name() : "");
        item.put("statusLabel", statusLabel(r.getStatus()));
        item.put("budgetText", formatVnd(r.getBudgetRef()));
        if (isPositive(r.getAgreedPrice())) {
            item.put("agreedPriceText", formatVnd(r.getAgreedPrice()));
        }
        if (isPositive(r.getDepositAmount())) {
            item.put("depositText", formatVnd(r.getDepositAmount()));
        }
        if (viewerRole == Role.CUSTOMER) {
            item.put("technicianName", d.technicianName() != null ? d.technicianName() : "Chưa chọn thợ");
        } else {
            item.put("customerName", nvl(d.customerName()));
        }
        if (r.getStatus() == RequestStatus.OPEN && r.getApplyDeadline() != null) {
            item.put("applyDeadline", formatDateTime(r.getApplyDeadline()));
        }
        item.put("createdAt", formatDateTime(r.getCreatedAt()));
        return item;
    }

    private ToolExecutionResult handleFindMatchingJobs(Map<String, Object> args, Long currentUserId, String currentRole) {
        if (currentUserId == null || parseRole(currentRole) != Role.TECHNICIAN) {
            return requireLogin("Chức năng này dành cho thợ đã đăng nhập tại /dang-nhap-tho.");
        }
        Page<RepairRequestResponse> page;
        try {
            page = repairRequestUseCase.getMatchingForTechnician(
                    currentUserId, 1, LIST_LIMIT, str(args.get("keyword")), "createdAt", "DESC");
        } catch (Exception ex) {
            return error(ex.getMessage() != null ? ex.getMessage()
                    : "Không lấy được bảng việc. Hồ sơ của bạn có thể chưa được duyệt KYC.");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (RepairRequestResponse r : page.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("requestCode", nvl(r.getRequestCode()));
            item.put("title", nvl(r.getTitle()));
            item.put("categoryName", nvl(r.getCategoryName()));
            item.put("areaName", nvl(r.getAreaName()));
            item.put("budgetText", formatVnd(r.getBudgetRef()));
            item.put("applyDeadline", formatDateTime(r.getApplyDeadline()));
            item.put("detailUrl", "/tho/yeu-cau/" + r.getId());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMatching", page.getTotalElements());
        result.put("items", items);
        result.put("note", "Thợ mở chi tiết yêu cầu để gửi báo giá (công + vật tư); khách so sánh các báo giá và chọn thợ. "
                + "Chatbot không gửi báo giá hộ.");
        return ok(result);
    }

    // ---------------------------------------------------------------- thợ

    private ToolExecutionResult handleGetRequestQuotations(Map<String, Object> args, Long currentUserId, String currentRole) {
        Role role = parseRole(currentRole);
        if (currentUserId == null || (role != Role.CUSTOMER && role != Role.TECHNICIAN)) {
            return requireLogin("Bạn cần đăng nhập để xem báo giá của yêu cầu sửa chữa.");
        }
        Long requestId = parseLong(args.get("request_id"));
        String requestCode = str(args.get("request_code"));
        if (requestId == null && requestCode != null) {
            Matcher m = REQUEST_CODE_PATTERN.matcher(requestCode);
            requestId = findRequestIdByCode(m.find() ? m.group().toUpperCase(Locale.ROOT)
                    : requestCode.trim().toUpperCase(Locale.ROOT), currentUserId, role);
        }
        if (requestId == null) {
            return error("Cần ID yêu cầu (số) hoặc mã yêu cầu dạng REQ-YYYYMMDD-XXXXXX.");
        }

        List<QuotationResponse> quotes;
        try {
            quotes = quotationUseCase.getQuotationsForRequest(requestId, currentUserId, role.name());
        } catch (Exception ex) {
            return error("Không xem được báo giá của yêu cầu này (chỉ chủ đơn hoặc thợ phù hợp mới xem được).");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal lowest = null;
        long pending = 0;
        for (QuotationResponse q : quotes) {
            BigDecimal total = q.getTotalPrice() != null ? q.getTotalPrice()
                    : nz(q.getPriceLaborVnd()).add(nz(q.getPriceMaterialsVnd()));
            String status = q.getStatus() != null ? q.getStatus().name() : "";
            if ("PENDING".equals(status)) {
                pending++;
                if (lowest == null || total.compareTo(lowest) < 0) {
                    lowest = total;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("technicianName", nvl(q.getTechnicianName()));
            if (Boolean.TRUE.equals(q.getIsMine())) {
                item.put("isMine", true);
            }
            if (q.getAvgRating() != null) {
                item.put("avgRating", q.getAvgRating().toPlainString());
            }
            if (q.getCompletedJobs() != null) {
                item.put("completedJobs", q.getCompletedJobs());
            }
            item.put("totalPriceText", formatVnd(total));
            item.put("laborText", formatVnd(q.getPriceLaborVnd()));
            if (isPositive(q.getPriceMaterialsVnd())) {
                item.put("materialsText", formatVnd(q.getPriceMaterialsVnd()));
            }
            if (q.getSolution() != null && !q.getSolution().isBlank()) {
                item.put("solution", truncate(q.getSolution(), 200));
            }
            item.put("status", status);
            item.put("statusLabel", switch (status) {
                case "PENDING" -> "Chờ khách chọn";
                case "ACCEPTED" -> "Đã được chọn";
                case "REJECTED" -> "Không được chọn";
                case "WITHDRAWN" -> "Thợ đã rút";
                default -> status;
            });
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", requestId);
        result.put("viewerRole", role.name());
        result.put("totalQuotations", items.size());
        result.put("pendingQuotations", pending);
        if (lowest != null) {
            result.put("lowestPendingPriceText", formatVnd(lowest));
        }
        result.put("quotations", items);
        result.put("detailUrl", (role == Role.TECHNICIAN ? "/tho/yeu-cau/" : "/yeu-cau-cua-toi/") + requestId);
        result.put("note", role == Role.CUSTOMER
                ? (items.isEmpty() ? "Chưa có thợ nào gửi báo giá. Đơn nhận báo giá tối đa 3 ngày."
                        : "Khách tự chọn trong trang chi tiết đơn bằng nút 'Chọn thợ này'; không bắt buộc chọn giá thấp nhất — "
                        + "nên cân nhắc cả phương án, điểm đánh giá và số việc đã làm. Chatbot không chọn thay khách.")
                : "Giá của thợ khác được hiển thị ẩn danh. Khi báo giá còn chờ duyệt bạn có thể sửa hoặc rút trong trang chi tiết.");
        return ok(result);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ToolExecutionResult handleGetMyTechnicianStatus(Long currentUserId, String currentRole) {
        if (currentUserId == null || parseRole(currentRole) != Role.TECHNICIAN) {
            return requireLogin("Bạn cần đăng nhập bằng tài khoản Thợ (tại /dang-nhap-tho) để kiểm tra hồ sơ.");
        }

        TechnicianProfile profile = technicianUseCase.getMyProfile(currentUserId);
        if (profile == null) {
            return ok(Map.of("message", "Chưa tìm thấy hồ sơ kỹ thuật viên."));
        }

        String verification = profile.getVerificationStatus() != null ? profile.getVerificationStatus().name() : "PENDING";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fullName", nvl(profile.getFullName()));
        result.put("verificationStatus", verification);
        result.put("verificationLabel", switch (verification) {
            case "APPROVED" -> "Đã xác minh — được gửi báo giá";
            case "REJECTED" -> "Bị từ chối — cần cập nhật hồ sơ theo lý do";
            default -> "Đang chờ Admin duyệt — chưa được gửi báo giá";
        });
        if (profile.getRejectionReason() != null && !profile.getRejectionReason().isBlank()) {
            result.put("rejectionReason", profile.getRejectionReason());
        }
        result.put("hasIdCardFront", profile.getIdCardFrontUrl() != null && !profile.getIdCardFrontUrl().isBlank());
        result.put("hasIdCardBack", profile.getIdCardBackUrl() != null && !profile.getIdCardBackUrl().isBlank());
        result.put("isOnline", Boolean.TRUE.equals(profile.getIsOnline()));
        result.put("completedJobs", profile.getCompletedJobs() != null ? profile.getCompletedJobs() : 0);
        result.put("avgRating", profile.getCompletedJobs() != null && profile.getCompletedJobs() > 0
                && profile.getAvgRating() != null ? profile.getAvgRating().toString() : "Chưa có đánh giá");
        result.put("profileUrl", "/tho/ho-so");
        return ok(result);
    }

    // ---------------------------------------------------------------- lịch hẹn

    private ToolExecutionResult handleGetMyAppointments(Long currentUserId, String currentRole) {
        if (currentUserId == null) {
            return requireLogin("Bạn cần đăng nhập để xem danh sách lịch hẹn của mình.");
        }

        List<AppointmentResponse> list = new ArrayList<>(appointmentUseCase.getMyAppointments(currentUserId, currentRole));
        list.sort(Comparator.comparing(AppointmentResponse::getScheduledDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> items = new ArrayList<>();
        for (AppointmentResponse a : list) {
            if (items.size() >= LIST_LIMIT) {
                break;
            }
            items.add(summarizeAppointment(a));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAppointments", list.size());
        result.put("appointments", items);
        if (items.isEmpty()) {
            result.put("message", "Bạn chưa có lịch hẹn nào. Lịch hẹn được tạo sau khi khách đã chọn thợ.");
        }
        return ok(result);
    }

    private Map<String, Object> summarizeAppointment(AppointmentResponse a) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId());
        item.put("requestCode", nvl(a.getRequestCode()));
        item.put("requestTitle", nvl(a.getRequestTitle()));
        item.put("appointmentType", nvl(a.getAppointmentType()));
        item.put("scheduledDate", a.getScheduledDate() != null ? a.getScheduledDate().toString() : "");
        item.put("scheduledTime", a.getScheduledTime() != null ? a.getScheduledTime().toString() : "");
        item.put("status", a.getStatus() != null ? a.getStatus().name() : "");
        item.put("statusLabel", nvl(a.getStatusLabel()));
        item.put("technicianName", nvl(a.getTechnicianName()));
        if (a.getCancelReason() != null) {
            item.put("cancelReason", a.getCancelReason());
        }
        return item;
    }

    // ---------------------------------------------------------------- phiếu hỗ trợ

    private ToolExecutionResult handleCreateSupportTicket(Map<String, Object> args, Long currentUserId, String guestContact) {
        String title = nvl(str(args.get("title")));
        String description = nvl(str(args.get("description")));
        String guestName = str(args.get("guest_name"));
        String contact = firstNonBlank(str(args.get("guest_contact")), guestContact);

        if (title.isBlank() || description.isBlank()) {
            return error("Tiêu đề và nội dung mô tả phiếu hỗ trợ không được để trống");
        }
        if (currentUserId == null && contact == null) {
            return ok(Map.of(
                    "requireContact", true,
                    "message", "Khách vãng lai cần cung cấp Số điện thoại hoặc Email để nhân viên CSKH liên hệ lại."
            ));
        }

        TicketPriority priority = TicketPriority.fromString(nvl(str(args.get("priority"))).isBlank()
                ? "Medium" : str(args.get("priority")));
        SupportTicket ticket = supportTicketUseCase.createTicket(
                currentUserId,
                guestName,
                contact,
                truncate(title, 200),
                truncate(description, 4000),
                priority,
                "Chưa phân công"
        );

        return ok(Map.of(
                "success", true,
                "ticketCode", ticket.getTicketCode(),
                "title", ticket.getTitle(),
                "status", ticket.getStatus().getValue(),
                "priority", ticket.getPriority().getValue(),
                "message", "Tạo phiếu hỗ trợ thành công. Mã phiếu: " + ticket.getTicketCode()
                        + ". Nhân viên CSKH sẽ liên hệ lại sớm nhất."
        ));
    }

    private ToolExecutionResult handleReadSupportTicket(
            Map<String, Object> args,
            Long currentUserId,
            String currentRole,
            String guestContact
    ) {
        String ticketCode = str(args.get("ticket_code"));
        String contact = firstNonBlank(str(args.get("guest_contact")), guestContact);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentRole);

        if (ticketCode != null) {
            Optional<SupportTicket> opt = supportTicketUseCase.getTicketByCode(
                    ticketCode.trim().toUpperCase(Locale.ROOT), currentUserId, isAdmin, contact);
            if (opt.isEmpty()) {
                return error("Không tìm thấy phiếu hỗ trợ hoặc bạn không có quyền xem phiếu này "
                        + "(khách vãng lai cần cung cấp đúng SĐT/email đã dùng khi tạo phiếu).");
            }
            SupportTicket t = opt.get();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ticketCode", t.getTicketCode());
            result.put("title", nvl(t.getTitle()));
            result.put("description", nvl(t.getDescription()));
            result.put("status", t.getStatus().getValue());
            result.put("priority", t.getPriority().getValue());
            result.put("assignee", nvl(t.getAssignee()));
            result.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
            return ok(result);
        }

        if (currentUserId == null) {
            return requireLogin("Hãy cho mình mã phiếu (TCK-XXXX) và SĐT/email đã dùng khi tạo phiếu, "
                    + "hoặc đăng nhập để xem danh sách phiếu của bạn.");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (SupportTicket t : supportTicketUseCase.getMyTickets(currentUserId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ticketCode", t.getTicketCode());
            item.put("title", nvl(t.getTitle()));
            item.put("status", t.getStatus().getValue());
            item.put("priority", t.getPriority().getValue());
            item.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
            items.add(item);
        }
        return ok(Map.of("totalTickets", items.size(), "tickets", items));
    }

    private ToolExecutionResult handleSearchKnowledgeBase(Map<String, Object> args, String currentRole) {
        String query = str(args.get("query"));
        if (query == null) {
            return ok(Map.of("content", ""));
        }
        var res = ragIndex.retrieve(query, currentRole);
        return ok(Map.of(
                "content", res.mergedContent().isBlank() ? "Không tìm thấy mục phù hợp trong kho tri thức." : res.mergedContent(),
                "sources", res.sources()
        ));
    }

    // ==================================================================================
    // NGHIỆP VỤ TRẠNG THÁI
    // ==================================================================================

    static boolean isCancellableByCustomer(RequestStatus status) {
        return status != null
                && status != RequestStatus.IN_PROGRESS
                && status != RequestStatus.AWAITING_ACCEPTANCE
                && status != RequestStatus.COMPLETED
                && status != RequestStatus.CANCELLED;
    }

    static String statusLabel(RequestStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case DRAFT -> "Bản nháp";
            case OPEN, BIDDING_OPEN, MATCHED_AWAITING_DEPOSIT -> "Đang tìm thợ";
            case ASSIGNED -> "Đã chọn thợ";
            case DEPOSIT_PAID -> "Đã đặt cọc";
            case INSPECTING -> "Thợ đang khảo sát";
            case AWAITING_COST_APPROVAL -> "Chờ khách duyệt chi phí phát sinh";
            case IN_PROGRESS -> "Đang sửa chữa";
            case AWAITING_ACCEPTANCE -> "Chờ khách nghiệm thu";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    static String nextStep(RequestStatus status, Role role) {
        boolean tech = role == Role.TECHNICIAN;
        if (status == null) {
            return "Theo dõi tiến độ trên hệ thống.";
        }
        return switch (status) {
            case DRAFT -> tech ? "Đơn đang là bản nháp của khách." : "Đơn đang là bản nháp: mở đơn và bấm 'Đăng yêu cầu' để thợ nhìn thấy.";
            case PENDING -> "Đơn đang chờ hệ thống duyệt trước khi mở nhận báo giá.";
            case OPEN, BIDDING_OPEN, MATCHED_AWAITING_DEPOSIT -> tech
                    ? "Đơn đang nhận báo giá: thợ đúng chuyên môn và khu vực gửi báo giá (sửa/rút khi còn chờ duyệt); khách sẽ so sánh và chọn thợ."
                    : "Đơn đang nhận báo giá (tối đa 3 ngày, quá hạn đơn tự hủy). Vào chi tiết đơn để so sánh các báo giá và bấm 'Chọn thợ này'. Chỉ sửa được nội dung đơn khi chưa có báo giá nào.";
            case ASSIGNED -> tech
                    ? "Bạn đã được khách chọn: đặt lịch hẹn khảo sát trên hệ thống, chờ khách xác nhận lịch và thanh toán cọc 30% qua QR rồi mới được bắt đầu khảo sát."
                    : "Bạn đã chọn thợ. Thợ sẽ đề xuất lịch hẹn: bấm 'Xác nhận lịch hẹn' trong chi tiết đơn, sau đó quét mã QR đặt cọc 30% và bấm xác nhận đã chuyển khoản. Nếu chưa ưng thợ, bạn có thể bấm 'Đổi thợ khác'.";
            case DEPOSIT_PAID -> tech
                    ? "Khách đã đặt cọc 30%. Đến đúng lịch hẹn và chuyển đơn sang 'Đang khảo sát'."
                    : "Bạn đã đặt cọc thành công, tiền cọc được FixLink giữ bảo chứng. Thợ sẽ đến theo lịch hẹn để khảo sát.";
            case INSPECTING -> tech
                    ? "Khảo sát xong: bắt đầu sửa theo ngân sách đã chốt, hoặc gửi đề xuất chi phí phát sinh để khách duyệt."
                    : "Thợ đang khảo sát. Nếu có lỗi phát sinh, thợ sẽ gửi chi phí mới để bạn duyệt trước khi sửa.";
            case AWAITING_COST_APPROVAL -> tech
                    ? "Chờ khách duyệt chi phí phát sinh; chỉ được sửa khi khách đồng ý."
                    : "Vào chi tiết đơn xem giải trình của thợ và bấm Đồng ý để thợ tiếp tục sửa. Nếu không đồng ý, bạn có thể 'Từ chối giá & Đổi thợ khác' hoặc hủy đơn.";
            case IN_PROGRESS -> tech
                    ? "Hoàn tất sửa chữa, chạy thử rồi báo hoàn thành để khách nghiệm thu."
                    : "Thợ đang sửa chữa. Đơn ở trạng thái này không thể hủy.";
            case AWAITING_ACCEPTANCE -> tech
                    ? "Chờ khách thanh toán phần còn lại qua QR, chấm sao và nghiệm thu."
                    : "Kiểm tra, vận hành thử thiết bị, quét mã QR thanh toán phần còn lại (giá chốt + phát sinh đã duyệt − tiền cọc), chấm 1–5 sao và viết nhận xét (bắt buộc) rồi hoàn tất nghiệm thu.";
            case COMPLETED -> "Đơn đã hoàn tất, thanh toán đã được giải ngân cho thợ.";
            case CANCELLED -> "Đơn đã hủy, không thể thao tác tiếp. Có thể đăng yêu cầu mới tại /dang-yeu-cau.";
        };
    }

    // ==================================================================================
    // TIỆN ÍCH
    // ==================================================================================

    private static Role parseRole(String role) {
        if (role == null) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static RequestStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("IN_PROGRESS".equals(normalized)) {
            return RequestStatus.IN_PROGRESS;
        }
        if ("ALL".equals(normalized) || "AWAITING_QUOTE".equals(normalized)) {
            return null;
        }
        try {
            return RequestStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String s = value.toString().trim();
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s).longValueExact();
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String nvl(String value) {
        return value != null ? value : "";
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "Chưa có";
        }
        return String.format(Locale.ROOT, "%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue())
                .replace(',', '.') + " đ";
    }

    private static String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(VN_DATE_TIME) : "";
    }

    private ToolExecutionResult ok(Object payload) {
        return new ToolExecutionResult(toJson(payload), false);
    }

    private ToolExecutionResult error(String message) {
        return new ToolExecutionResult(toJson(Map.of("isError", true, "message", message)), true);
    }

    private ToolExecutionResult requireLogin(String message) {
        return new ToolExecutionResult(toJson(Map.of("requireLogin", true, "message", message)), false);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
