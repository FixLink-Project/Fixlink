package com.fixlink.chatbot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackEntity;
import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackJpaRepository;
import com.fixlink.chatbot.application.port.in.ChatbotEngagementUseCase;
import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.domain.model.ChatCard;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.ChatSession;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import com.fixlink.domain.exception.DomainException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatbotEngagementService implements ChatbotEngagementUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ChatbotEngagementService.class);

    private static final Pattern REQUEST_DETAIL_ROUTE = Pattern.compile("^/(?:yeu-cau-cua-toi|tho/yeu-cau)/(\\d+)");
    private static final Pattern CONTACT = Pattern.compile("^(0[35789]\\d{8}|\\+84[35789]\\d{8}|[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+)$");
    private static final Pattern CRITICAL_WORDS = Pattern.compile(
            "\\b(lua dao|gian lan|mat tien|tru tien|bi tru|an cap|mat do|tranh chap|cuop|de doa|hanh hung)\\b");
    private static final Pattern HIGH_WORDS = Pattern.compile(
            "\\b(khieu nai|thai do|lam hong|lam hu|khong den|tre hen|bung|bi khoa|hack|hoan tien|boi thuong|den bu)\\b");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final int TRANSCRIPT_MESSAGES = 12;

    private final ChatbotToolPort toolPort;
    private final ChatCardFactory cardFactory;
    private final ConversationMemoryService memoryService;
    private final SupportTicketUseCase supportTicketUseCase;
    private final ChatbotFeedbackJpaRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    public ChatbotEngagementService(
            ChatbotToolPort toolPort,
            ChatCardFactory cardFactory,
            ConversationMemoryService memoryService,
            SupportTicketUseCase supportTicketUseCase,
            ChatbotFeedbackJpaRepository feedbackRepository,
            ObjectMapper objectMapper
    ) {
        this.toolPort = toolPort;
        this.cardFactory = cardFactory;
        this.memoryService = memoryService;
        this.supportTicketUseCase = supportTicketUseCase;
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
    }

    // ==================================================================================
    // LỜI CHÀO THEO NGỮ CẢNH TRANG
    // ==================================================================================

    @Override
    public WelcomeResponse getWelcome(String route, Long userId, String role) {
        String r = route != null ? route.trim() : "/";
        String uRole = role != null ? role.toUpperCase(Locale.ROOT) : "GUEST";
        try {
            Matcher detail = REQUEST_DETAIL_ROUTE.matcher(r);
            if (userId != null && detail.find()) {
                WelcomeResponse w = requestDetailWelcome(Long.parseLong(detail.group(1)), userId, uRole);
                if (w != null) {
                    return w;
                }
            }
            if (userId != null && "TECHNICIAN".equals(uRole) && r.startsWith("/tho")) {
                WelcomeResponse w = technicianWelcome(userId);
                if (w != null) {
                    return w;
                }
            }
            if (userId != null && "CUSTOMER".equals(uRole)
                    && (r.startsWith("/yeu-cau-cua-toi") || r.startsWith("/khach-hang"))) {
                WelcomeResponse w = customerOrdersWelcome(userId);
                if (w != null) {
                    return w;
                }
            }
        } catch (Exception ex) {
            LOG.warn("Không dựng được lời chào theo ngữ cảnh cho route {}: {}", r, ex.getMessage());
        }

        if (r.startsWith("/dang-yeu-cau")) {
            return new WelcomeResponse(
                    "👋 Bạn đang tạo yêu cầu sửa chữa. Mô tả cho mình **thiết bị** và **hiện tượng** (ví dụ: \"máy lạnh chỉ thổi gió, "
                            + "không mát\"), mình sẽ gợi ý đúng danh mục, giá tham khảo và cách đặt ngân sách hợp lý.",
                    List.of("Máy lạnh không mát chọn mục nào?", "Nên đặt ngân sách bao nhiêu?", "Cần chụp ảnh thế nào?",
                            "Bao lâu thì có thợ gửi báo giá?"),
                    List.of(), List.of());
        }
        if ("TECHNICIAN".equals(uRole)) {
            return new WelcomeResponse(
                    "👋 Chào bạn! Mình hỗ trợ thợ FixLink: kiểm tra hồ sơ KYC, tìm việc phù hợp, quy trình khảo sát – báo phát sinh – nghiệm thu.",
                    List.of("Hồ sơ của tôi đã được duyệt chưa?", "Có việc nào phù hợp với tôi?", "Lịch hẹn của tôi",
                            "Cách gửi báo giá cạnh tranh?"),
                    List.of(), List.of());
        }
        if ("CUSTOMER".equals(uRole)) {
            return new WelcomeResponse(
                    "👋 Chào bạn! Mình là **Trợ lý FixLink**. Mình có thể tư vấn sửa thiết bị, tra cứu đơn và lịch hẹn của bạn, "
                            + "giải thích cọc – nghiệm thu, hoặc chuyển bạn tới nhân viên CSKH.",
                    List.of("Tư vấn sửa thiết bị đang hỏng", "Đơn của tôi đang ở trạng thái nào?", "Lịch hẹn sắp tới của tôi",
                            "Gặp nhân viên hỗ trợ"),
                    List.of(), List.of());
        }
        return new WelcomeResponse(
                "👋 **Xin chào!** Mình là **Trợ lý FixLink** – hỗ trợ 24/7.\n\nMình có thể giúp bạn:\n"
                        + "- 🔧 Tư vấn sự cố & chọn đúng dịch vụ sửa chữa\n"
                        + "- 💰 Báo giá tham khảo, tính tiền cọc\n"
                        + "- 📝 Chuẩn bị yêu cầu sửa chữa điền sẵn\n"
                        + "- 📦 Tra cứu đơn, lịch hẹn (sau khi đăng nhập)\n"
                        + "- 👤 Chuyển nhân viên CSKH khi cần\n\nBạn đang cần hỗ trợ điều gì?",
                List.of("Tư vấn sửa thiết bị đang hỏng", "Bảng giá tham khảo dịch vụ", "Tiền cọc và thanh toán thế nào?",
                        "FixLink là gì, có uy tín không?"),
                List.of(), List.of());
    }

    private WelcomeResponse requestDetailWelcome(Long requestId, Long userId, String role) {
        var result = toolPort.executeTool("get_repair_request_status", Map.of("request_id", requestId), userId, role, null);
        JsonNode node = read(result.content());
        if (result.isError() || node == null || !(node.has("requestId") || node.has("id"))) {
            return null;
        }
        ChatCard card = cardFactory.requestCard(node, true, null);
        String status = node.path("status").asText("");
        String message = "👋 Mình thấy bạn đang xem đơn **" + node.path("requestCode").asText("") + "**: hiện ở trạng thái **"
                + node.path("statusLabel").asText(status) + "**.\n\n**Bước tiếp theo:** " + node.path("nextStep").asText("");
        List<String> suggestions = switch (status) {
            case "OPEN", "DRAFT", "PENDING" -> List.of("Đơn này đã có những báo giá nào?", "Nên chọn thợ nào?",
                    "Hủy đơn này có mất phí không?", "Nên tăng ngân sách không?");
            case "ASSIGNED" -> List.of("Lịch hẹn của đơn này thế nào?", "Thợ chưa liên hệ thì sao?",
                    "Tiền cọc của đơn này là bao nhiêu?", "Tôi muốn đổi thợ");
            case "INSPECTING", "AWAITING_COST_APPROVAL" -> List.of("Có nên đồng ý chi phí phát sinh?",
                    "Không đồng ý phát sinh thì sao?", "Thợ đòi trả tiền mặt thì sao?");
            case "IN_PROGRESS" -> List.of("Sửa xong thì làm gì tiếp?", "Đang sửa có hủy được không?");
            case "AWAITING_ACCEPTANCE" -> List.of("Đánh giá và nghiệm thu thế nào?", "Thiết bị vẫn còn lỗi thì sao?",
                    "Sau nghiệm thu có bảo hành không?");
            case "COMPLETED" -> List.of("Sửa xong lại hỏng thì sao?", "Đăng yêu cầu mới", "Gặp nhân viên hỗ trợ");
            default -> List.of("Đăng yêu cầu mới", "Gặp nhân viên hỗ trợ");
        };
        List<ChatCard> cards = new ArrayList<>(List.of(card));
        if ("OPEN".equals(status) || "BIDDING_OPEN".equals(status)) {
            // Đơn đang nhận báo giá: hiện luôn bảng so sánh báo giá như thẻ đơn hàng của các sàn TMĐT
            var quotes = toolPort.executeTool("get_request_quotations", Map.of("request_id", requestId), userId, role, null);
            JsonNode q = read(quotes.content());
            if (!quotes.isError() && q != null) {
                int total = q.path("totalQuotations").asInt(0);
                message += total == 0
                        ? "\n\nHiện **chưa có thợ nào gửi báo giá**."
                        : "\n\nĐơn đã có **" + total + "** báo giá – xem bảng so sánh bên dưới rồi chọn thợ phù hợp nhé.";
                cards.addAll(cardFactory.fromToolTraces(
                        List.of(new ToolCallRecorder.ToolTrace("get_request_quotations", quotes.content(), false))));
            }
        }
        return new WelcomeResponse(message, suggestions, cards, List.of());
    }

    private WelcomeResponse technicianWelcome(Long userId) {
        var result = toolPort.executeTool("get_my_technician_status", Map.of(), userId, "TECHNICIAN", null);
        JsonNode node = read(result.content());
        if (result.isError() || node == null || !node.has("verificationStatus")) {
            return null;
        }
        String status = node.path("verificationStatus").asText();
        String message = switch (status) {
            case "APPROVED" -> "👋 Chào " + node.path("fullName").asText("bạn") + "! Hồ sơ của bạn **đã được xác minh** – "
                    + "bạn có thể gửi báo giá cho việc đúng chuyên môn và khu vực. Hỏi mình \"có việc nào phù hợp\" để xem nhanh nhé.";
            case "REJECTED" -> "👋 Hồ sơ KYC của bạn **đang bị từ chối**. Xem lý do bên dưới và cập nhật giấy tờ tại trang hồ sơ thợ.";
            default -> "👋 Hồ sơ của bạn **đang chờ Admin duyệt** – trong thời gian này bạn chưa gửi báo giá được.";
        };
        List<String> suggestions = "APPROVED".equals(status)
                ? List.of("Có việc nào phù hợp với tôi?", "Lịch hẹn của tôi", "Làm sao để được khách chọn nhiều hơn?")
                : List.of("Hồ sơ KYC cần những gì?", "Bao lâu thì được duyệt?", "Gặp nhân viên hỗ trợ");
        return new WelcomeResponse(message, suggestions, List.of(cardFactory.fromToolTraces(
                List.of(new ToolCallRecorder.ToolTrace("get_my_technician_status", result.content(), false))).get(0)),
                List.of());
    }

    private WelcomeResponse customerOrdersWelcome(Long userId) {
        var result = toolPort.executeTool("get_my_repair_requests", Map.of(), userId, "CUSTOMER", null);
        JsonNode node = read(result.content());
        if (result.isError() || node == null || !node.has("items")) {
            return null;
        }
        List<JsonNode> needAction = new ArrayList<>();
        for (JsonNode item : node.path("items")) {
            String s = item.path("status").asText();
            if ("AWAITING_COST_APPROVAL".equals(s) || "AWAITING_ACCEPTANCE".equals(s)) {
                needAction.add(item);
            }
        }
        if (needAction.isEmpty()) {
            return new WelcomeResponse(
                    "👋 Chào bạn! Bạn có **" + node.path("totalItems").asText("0") + "** yêu cầu sửa chữa. Hỏi mình trạng thái đơn, "
                            + "lịch hẹn, hoặc tư vấn sự cố mới nhé.",
                    List.of("Đơn của tôi đang ở trạng thái nào?", "Lịch hẹn sắp tới của tôi", "Tư vấn sửa thiết bị đang hỏng"),
                    List.of(), List.of());
        }
        List<ChatCard> cards = new ArrayList<>();
        for (JsonNode item : needAction) {
            if (cards.size() >= 2) {
                break;
            }
            cards.add(cardFactory.requestCard(item, false, "CUSTOMER"));
        }
        return new WelcomeResponse(
                "🔔 Bạn có **" + needAction.size() + "** đơn đang **chờ bạn thao tác** (duyệt chi phí phát sinh hoặc nghiệm thu). "
                        + "Mở đơn bên dưới để xử lý nhé.",
                List.of("Có nên đồng ý chi phí phát sinh?", "Đánh giá và nghiệm thu thế nào?", "Thiết bị vẫn còn lỗi thì sao?"),
                cards, List.of());
    }

    // ==================================================================================
    // CHUYỂN NHÂN VIÊN MỘT CHẠM
    // ==================================================================================

    @Override
    public HandoffResult requestHandoff(HandoffCommand command) {
        String issue = command.issue() != null ? command.issue().trim() : "";
        if (issue.length() < 5) {
            throw new DomainException("HANDOFF_ISSUE_REQUIRED", "Vui lòng mô tả ngắn gọn vấn đề cần hỗ trợ (ít nhất 5 ký tự).", 400);
        }
        String contact = command.contact() != null ? command.contact().trim().replace(" ", "") : null;
        if (command.userId() == null) {
            if (contact == null || contact.isBlank()) {
                throw new DomainException("HANDOFF_CONTACT_REQUIRED",
                        "Vui lòng để lại số điện thoại hoặc email để nhân viên CSKH liên hệ lại.", 400);
            }
            if (!CONTACT.matcher(contact).matches()) {
                throw new DomainException("HANDOFF_CONTACT_INVALID",
                        "Số điện thoại (10 số, đầu 03/05/07/08/09) hoặc email chưa đúng định dạng.", 400);
            }
        }

        TicketPriority priority = inferPriority(issue);
        String title = "[Chatbot] " + (issue.length() > 80 ? issue.substring(0, 80) + "..." : issue);
        String description = issue + transcriptFor(command.sessionId(), command.userId());

        SupportTicket ticket = supportTicketUseCase.createTicket(
                command.userId(),
                command.name() != null && !command.name().isBlank() ? command.name().trim() : null,
                contact,
                title,
                description.length() > 4000 ? description.substring(0, 4000) : description,
                priority,
                "Chưa phân công");

        String message = "Đã chuyển yêu cầu của bạn tới nhân viên CSKH với mã phiếu **" + ticket.getTicketCode()
                + "** (mức ưu tiên: " + priorityLabel(priority) + "). Nhân viên đã nhận kèm toàn bộ nội dung cuộc trò chuyện, "
                + "bạn không cần kể lại từ đầu.";
        appendToSession(command.sessionId(), message, ticket.getTicketCode());
        LOG.info("Chatbot handoff created ticket {} (priority {}) for session {}", ticket.getTicketCode(), priority, command.sessionId());
        return new HandoffResult(ticket.getTicketCode(), ticket.getPriority().getValue(), message);
    }

    static TicketPriority inferPriority(String issue) {
        String text = SymptomClassifier.normalize(issue);
        if (CRITICAL_WORDS.matcher(text).find() || SymptomClassifier.isDanger(issue)) {
            return TicketPriority.CRITICAL;
        }
        if (HIGH_WORDS.matcher(text).find()) {
            return TicketPriority.HIGH;
        }
        return TicketPriority.MEDIUM;
    }

    private static String priorityLabel(TicketPriority priority) {
        return switch (priority) {
            case CRITICAL -> "Khẩn cấp";
            case HIGH -> "Cao";
            case MEDIUM -> "Trung bình";
            case LOW -> "Thấp";
        };
    }

    /** Đính kèm lịch sử hội thoại gần nhất để nhân viên nắm bối cảnh (chỉ khi phiên thuộc đúng người yêu cầu). */
    private String transcriptFor(String sessionId, Long userId) {
        ChatSession session = memoryService.getSession(sessionId);
        if (session == null || (session.getUserId() != null && !session.getUserId().equals(userId))) {
            return "";
        }
        List<ChatMessage> history = memoryService.getHistory(sessionId);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n--- Lịch sử trò chuyện với Trợ lý FixLink ---");
        for (ChatMessage m : history.subList(Math.max(0, history.size() - TRANSCRIPT_MESSAGES), history.size())) {
            String who = "user".equalsIgnoreCase(m.getRole()) ? "Khách" : "Trợ lý";
            String content = m.getContent() != null ? m.getContent().replaceAll("\\s+", " ").trim() : "";
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            sb.append("\n[").append(m.getTimestamp() != null ? TIME.format(m.getTimestamp()) : "").append("] ")
                    .append(who).append(": ").append(content);
        }
        return sb.toString();
    }

    private void appendToSession(String sessionId, String message, String ticketCode) {
        if (sessionId == null || memoryService.getSession(sessionId) == null) {
            return;
        }
        memoryService.addMessage(sessionId, ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .role("model")
                .content(message)
                .ticketId(ticketCode)
                .needsHuman(true)
                .modelUsed("handoff")
                .timestamp(Instant.now())
                .build());
    }

    // ==================================================================================
    // ĐO LƯỜNG CHẤT LƯỢNG
    // ==================================================================================

    @Override
    public Map<String, Object> getQualityStats() {
        List<ChatbotFeedbackEntity> all = feedbackRepository.findAll();
        long up = 0;
        long down = 0;
        long csatCount = 0;
        long csatSum = 0;
        long csatSatisfied = 0;
        Map<String, Long> csatDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            csatDistribution.put(String.valueOf(i), 0L);
        }
        for (ChatbotFeedbackEntity f : all) {
            String rating = f.getRating() != null ? f.getRating() : "";
            if ("UP".equals(rating)) {
                up++;
            } else if ("DOWN".equals(rating)) {
                down++;
            } else if (rating.startsWith("CSAT_")) {
                try {
                    int score = Integer.parseInt(rating.substring(5));
                    if (score >= 1 && score <= 5) {
                        csatCount++;
                        csatSum += score;
                        if (score >= 4) {
                            csatSatisfied++;
                        }
                        csatDistribution.merge(String.valueOf(score), 1L, Long::sum);
                    }
                } catch (NumberFormatException ignored) {
                    // bỏ qua bản ghi không hợp lệ
                }
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("thumbsUp", up);
        stats.put("thumbsDown", down);
        stats.put("helpfulRate", up + down == 0 ? null : Math.round(up * 1000.0 / (up + down)) / 10.0);
        stats.put("csatResponses", csatCount);
        stats.put("csatAverage", csatCount == 0 ? null : Math.round(csatSum * 100.0 / csatCount) / 100.0);
        stats.put("csatSatisfiedRate", csatCount == 0 ? null : Math.round(csatSatisfied * 1000.0 / csatCount) / 10.0);
        stats.put("csatDistribution", csatDistribution);
        return stats;
    }

    private JsonNode read(String json) {
        try {
            return json != null ? objectMapper.readTree(json) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
