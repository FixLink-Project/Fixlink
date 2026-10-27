package com.fixlink.chatbot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort.ToolExecutionResult;
import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Trả lời câu hỏi về dữ liệu cá nhân (đơn, lịch hẹn, hồ sơ thợ, việc phù hợp) bằng cách gọi thẳng tool khi
 * mọi nhà cung cấp LLM đều hết hạn mức. Khách đã đăng nhập vẫn tra được đơn thật thay vì nhận câu xin lỗi.
 */
@Component
public class OfflinePersonalResponder {

    static final String MODEL = "local-data";

    private static final Pattern REQUEST_CODE = Pattern.compile("\\bREQ-[A-Z0-9-]{4,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MY_REQUESTS = Pattern.compile(
            "\\b(don cua toi|don hang|cac don|may don|bao nhieu don|don nao|yeu cau cua toi|don dang|don cua minh|don minh"
                    + "|bao nhieu yeu cau|may yeu cau|yeu cau cua minh|yeu cau sua chua cua toi|toi da co|toi co bao nhieu"
                    + "|danh sach don|danh sach yeu cau)\\b");
    private static final Pattern APPOINTMENTS = Pattern.compile("\\b(lich hen|lich khao sat|hen gio|lich sua)\\b");
    private static final Pattern QUOTES = Pattern.compile("\\b(bao gia|so sanh|chon tho nao)\\b");
    private static final Pattern TECH_PROFILE = Pattern.compile("\\b(ho so|duyet|kyc|xac minh|tai khoan tho)\\b");
    private static final Pattern TECH_JOBS = Pattern.compile("\\b(viec|cong viec|job|don moi|nhan viec)\\b");
    private static final Pattern SWITCH = Pattern.compile("\\b(doi tho|thay tho|tho khac)\\b");
    private static final Pattern CANCEL = Pattern.compile("\\b(huy|cancel)\\b");
    private static final Pattern DEPOSIT = Pattern.compile("\\b(coc|dat coc|tien coc)\\b");

    /** Trạng thái được đổi thợ: theo QuotationService.switchTechnician. */
    private static final Set<String> SWITCHABLE = Set.of(
            "ASSIGNED", "DEPOSIT_PAID", "INSPECTING", "AWAITING_COST_APPROVAL");
    /** Trạng thái không được hủy: đã sửa, chờ nghiệm thu hoặc đã đóng. */
    private static final Set<String> NOT_CANCELLABLE = Set.of(
            "IN_PROGRESS", "AWAITING_ACCEPTANCE", "COMPLETED", "CANCELLED");

    static String switchAnswer(String status) {
        if (SWITCHABLE.contains(status)) {
            return "**Được đổi thợ.** Bạn mở chi tiết đơn, bấm **\"Đổi thợ khác\"**, chọn lý do rồi chọn báo giá dự phòng "
                    + "của thợ khác hoặc *Mở lại sàn nhận báo giá*. Tiền cọc đã đóng (nếu có) được giữ bảo chứng và bảo lưu cho thợ mới.";
        }
        if ("OPEN".equals(status) || "DRAFT".equals(status) || "PENDING".equals(status)) {
            return "Đơn này **chưa chọn thợ**, bạn chỉ cần chọn báo giá phù hợp bằng nút **\"Chọn thợ này\"**.";
        }
        return "**Không đổi được thợ** ở trạng thái hiện tại: chỉ đổi được khi đơn chưa chuyển sang *Đang sửa chữa*.";
    }

    /** Tiền cọc của một đơn: lấy số đã ghi nhận, chưa có thì tính 30% giá chốt (làm tròn lên). */
    static String depositAnswer(JsonNode n) {
        String deposit = n.path("depositText").asText("");
        if (!deposit.isBlank()) {
            return "Tiền cọc của đơn này là **" + deposit + "** (30% giá chốt), thanh toán qua mã QR trong chi tiết đơn.";
        }
        String digits = n.path("agreedPriceText").asText("").replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "Đơn này **chưa có giá chốt** nên chưa tính tiền cọc; cọc = 30% báo giá bạn chọn.";
        }
        long deposit30 = (long) Math.ceil(Long.parseLong(digits) * 0.3);
        return "Tiền cọc dự kiến của đơn này là **" + String.format(Locale.ROOT, "%,d", deposit30).replace(',', '.')
                + " đ** (30% giá chốt). Mã QR đặt cọc hiện trong chi tiết đơn sau khi bạn xác nhận lịch hẹn.";
    }

    static String cancelAnswer(String status) {
        if (NOT_CANCELLABLE.contains(status)) {
            return "**Không hủy được** đơn ở trạng thái hiện tại (chỉ hủy được trước khi thợ bắt đầu sửa).";
        }
        return "**Được hủy.** Bạn mở chi tiết đơn, bấm *Hủy yêu cầu* và nhập lý do (trợ lý không hủy thay bạn). "
                + "Tỷ lệ hoàn cọc khi hủy chưa được công bố; tranh chấp do Admin thẩm định qua phiếu hỗ trợ.";
    }

    private final ChatbotToolPort toolPort;
    private final ObjectMapper objectMapper;

    public OfflinePersonalResponder(ChatbotToolPort toolPort, ObjectMapper objectMapper) {
        this.toolPort = toolPort;
        this.objectMapper = objectMapper;
    }

    /** Câu hỏi về dữ liệu riêng của người hỏi (đơn, mã đơn, lịch hẹn) — cần đăng nhập mới tra được. */
    public static boolean asksPersonalData(String message) {
        if (message == null) {
            return false;
        }
        String text = SymptomClassifier.normalize(message);
        return REQUEST_CODE.matcher(message).find() || MY_REQUESTS.matcher(text).find()
                || (APPOINTMENTS.matcher(text).find() && text.matches(".*\\b(cua toi|cua minh|toi co|minh co)\\b.*"));
    }

    /**
     * Câu hỏi tổng quan về đơn của mình ("tôi có bao nhiêu đơn", "danh sách đơn của tôi"): trả lời thẳng từ dữ liệu,
     * không cần AI — AI hay tra nhiều lần với bộ lọc khác nhau rồi trả lời lệch câu hỏi.
     */
    public static boolean asksRequestOverview(String message) {
        return message != null && !REQUEST_CODE.matcher(message).find()
                && MY_REQUESTS.matcher(SymptomClassifier.normalize(message)).find();
    }

    /** Khách vãng lai hỏi về đơn/lịch hẹn của mình: hướng dẫn đăng nhập thay vì để AI hứa "đang kiểm tra". */
    public static ChatOrchestrationResult loginRequired() {
        return new ChatOrchestrationResult(
                "Để xem các yêu cầu sửa chữa, lịch hẹn và trạng thái đơn **của bạn**, bạn cần **đăng nhập** trước nhé "
                        + "(khách hàng tại /dang-nhap, thợ tại /dang-nhap-tho). Đăng nhập xong, bạn hỏi lại mình sẽ tra ngay "
                        + "số lượng đơn, trạng thái và bước tiếp theo của từng đơn.\n\nChưa có tài khoản? Đăng ký miễn phí tại /dang-ky.",
                List.of("Cách đăng ký tài khoản", "Cách đăng yêu cầu sửa chữa"),
                List.of(Map.of("type", "link", "label", "Đăng nhập", "url", "/dang-nhap"),
                        Map.of("type", "link", "label", "Đăng ký", "url", "/dang-ky")),
                false, null, "local-data");
    }

    /** AI chỉ hứa "đang kiểm tra / sẽ tra cứu" mà không đưa dữ liệu: câu trả lời rỗng, cần thay bằng dữ liệu thật. */
    public static boolean isEmptyPromise(String reply) {
        if (reply == null) {
            return true;
        }
        String text = SymptomClassifier.normalize(reply);
        return reply.length() < 220 && text.matches(".*\\b(dang|se|de minh) (kiem tra|tra cuu|xem|lay)\\b.*")
                && !reply.matches("(?s).*\\d.*");
    }

    public Optional<ChatOrchestrationResult> answer(String message, Long userId, String role) {
        if (userId == null || message == null) {
            return Optional.empty();
        }
        String r = role != null ? role.toUpperCase(Locale.ROOT) : "";
        boolean technician = "TECHNICIAN".equals(r);
        if (!technician && !"CUSTOMER".equals(r)) {
            return Optional.empty();
        }
        String text = SymptomClassifier.normalize(message);
        Matcher code = REQUEST_CODE.matcher(message);

        if (code.find()) {
            String requestCode = code.group().toUpperCase(Locale.ROOT);
            if (QUOTES.matcher(text).find()) {
                return call("get_request_quotations", Map.of("request_code", requestCode), userId, r)
                        .map(node -> reply(quotesText(requestCode, node), List.of("Nên chọn thợ nào?", "Tiền cọc tính thế nào?")));
            }
            boolean asksSwitch = SWITCH.matcher(text).find();
            boolean asksCancel = CANCEL.matcher(text).find();
            boolean asksDeposit = DEPOSIT.matcher(text).find();
            return call("get_repair_request_status", Map.of("request_code", requestCode), userId, r)
                    .map(node -> {
                        String status = node.path("status").asText("");
                        String answer = statusText(node);
                        if (asksSwitch) {
                            answer = switchAnswer(status) + "\n\n" + answer;
                        } else if (asksCancel) {
                            answer = cancelAnswer(status) + "\n\n" + answer;
                        } else if (asksDeposit) {
                            answer = depositAnswer(node) + "\n\n" + answer;
                        }
                        return reply(answer, List.of("Đổi thợ được không?", "Hủy đơn có mất cọc không?"));
                    });
        }
        if (technician && TECH_PROFILE.matcher(text).find()) {
            return call("get_my_technician_status", Map.of(), userId, r)
                    .map(node -> reply(techProfileText(node), List.of("Có việc nào phù hợp với tôi?", "Thợ gửi báo giá thế nào?")));
        }
        if (technician && TECH_JOBS.matcher(text).find() && !MY_REQUESTS.matcher(text).find()) {
            return call("find_matching_jobs", Map.of(), userId, r)
                    .map(node -> reply(jobsText(node), List.of("Báo giá vượt ngân sách được không?", "Hồ sơ của tôi đã duyệt chưa?")));
        }
        if (APPOINTMENTS.matcher(text).find()) {
            return call("get_my_appointments", Map.of(), userId, r)
                    .map(node -> reply(appointmentsText(node), List.of("Dời lịch hẹn thế nào?", "Thợ không đến thì sao?")));
        }
        if (MY_REQUESTS.matcher(text).find()) {
            return call("get_my_repair_requests", Map.of(), userId, r)
                    .map(node -> reply(requestsText(node, technician), List.of("Đơn nào cần tôi thao tác?", "Đăng yêu cầu mới")));
        }
        return Optional.empty();
    }

    private Optional<JsonNode> call(String tool, Map<String, Object> args, Long userId, String role) {
        ToolExecutionResult result = toolPort.executeTool(tool, new HashMap<>(args), userId, role, null);
        if (result == null || result.content() == null) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(result.content());
            if (result.isError() && !node.hasNonNull("error") && !node.hasNonNull("message")) {
                return Optional.empty();
            }
            return Optional.of(node);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static ChatOrchestrationResult reply(String text, List<String> suggestions) {
        return new ChatOrchestrationResult(text, suggestions, List.of(), false, null, MODEL);
    }

    private static String errorOr(JsonNode node, String fallback) {
        if (node.hasNonNull("error")) {
            return node.path("error").asText();
        }
        return fallback;
    }

    static String statusText(JsonNode n) {
        if (!n.hasNonNull("requestCode") && !n.hasNonNull("status")) {
            return errorOr(n, "Mình chưa tìm thấy đơn này trong tài khoản của bạn. Bạn kiểm tra lại mã đơn tại *Yêu cầu của tôi* nhé.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Đơn **").append(n.path("requestCode").asText()).append("**");
        if (n.hasNonNull("title")) {
            sb.append(" – ").append(n.path("title").asText());
        }
        sb.append(" đang ở trạng thái **").append(n.path("statusLabel").asText(n.path("status").asText())).append("**.");
        appendField(sb, "Giá chốt", n.path("agreedPriceText").asText(""));
        appendField(sb, "Tiền cọc (30%)", n.path("depositText").asText(""));
        appendField(sb, "Thợ phụ trách", n.path("technicianName").asText(""));
        if (n.hasNonNull("nextStep")) {
            sb.append("\n\n**Bước tiếp theo:** ").append(n.path("nextStep").asText());
        }
        return sb.toString();
    }

    static String requestsText(JsonNode n, boolean technician) {
        JsonNode items = n.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return errorOr(n, n.path("message").asText(technician
                    ? "Bạn chưa được khách chọn cho việc nào."
                    : "Bạn chưa có yêu cầu sửa chữa nào. Có thể đăng yêu cầu mới tại /dang-yeu-cau."));
        }
        StringBuilder sb = new StringBuilder();
        if (technician) {
            sb.append("Bạn có **").append(n.path("totalItems").asInt(items.size())).append("** việc đã được khách chọn");
        } else {
            sb.append("Bạn hiện có tổng cộng **").append(n.path("totalItems").asInt(items.size())).append("** yêu cầu sửa chữa");
        }
        JsonNode groups = n.path("countsByGroup");
        if (groups.isObject() && groups.size() > 0) {
            sb.append(":\n");
            groups.fields().forEachRemaining(g -> sb.append("- ").append(g.getKey()).append(": **")
                    .append(g.getValue().asLong()).append("**\n"));
            sb.append("\n**Gần nhất:**\n");
        } else {
            sb.append(", gần nhất:\n");
        }
        int i = 1;
        for (JsonNode it : items) {
            if (i > 6) {
                break;
            }
            sb.append(i++).append(". **").append(it.path("requestCode").asText()).append("** – ")
                    .append(it.path("title").asText()).append(": ").append(it.path("statusLabel").asText()).append("\n");
        }
        sb.append("\nHỏi mình mã đơn cụ thể (ví dụ \"").append(items.get(0).path("requestCode").asText())
                .append(" đang thế nào?\") để xem chi tiết và bước tiếp theo.");
        return sb.toString();
    }

    static String appointmentsText(JsonNode n) {
        JsonNode items = n.path("appointments");
        if (!items.isArray() || items.isEmpty()) {
            return errorOr(n, n.path("message").asText("Bạn chưa có lịch hẹn nào."));
        }
        StringBuilder sb = new StringBuilder("Lịch hẹn của bạn:\n");
        int i = 1;
        for (JsonNode a : items) {
            if (i > 6) {
                break;
            }
            sb.append(i++).append(". **").append(a.path("requestCode").asText()).append("** – ")
                    .append(a.path("scheduledDate").asText()).append(" ").append(a.path("scheduledTime").asText())
                    .append(": ").append(a.path("statusLabel").asText()).append("\n");
        }
        sb.append("\nLịch ở trạng thái *Chờ xác nhận* thì khách bấm **Xác nhận lịch hẹn** trong chi tiết đơn; "
                + "xác nhận xong mã QR đặt cọc 30% sẽ hiện ra.");
        return sb.toString();
    }

    static String techProfileText(JsonNode n) {
        if (!n.hasNonNull("verificationStatus")) {
            return errorOr(n, "Mình chưa đọc được hồ sơ thợ của bạn. Bạn mở /tho/ho-so để kiểm tra nhé.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Hồ sơ thợ **").append(n.path("fullName").asText()).append("**: **")
                .append(n.path("verificationLabel").asText(n.path("verificationStatus").asText())).append("**.");
        if (n.hasNonNull("rejectionReason")) {
            sb.append("\n- Lý do từ chối: ").append(n.path("rejectionReason").asText());
        }
        sb.append("\n- Việc đã hoàn thành: ").append(n.path("completedJobs").asInt());
        if (n.hasNonNull("avgRating")) {
            sb.append("\n- Điểm đánh giá: ").append(n.path("avgRating").asText());
        }
        switch (n.path("verificationStatus").asText()) {
            case "APPROVED" -> sb.append("\n\nHồ sơ **đã được Admin duyệt**: bạn được gửi báo giá cho các việc đúng chuyên môn và khu vực đã đăng ký.");
            case "PENDING" -> sb.append("\n\nHồ sơ **đang chờ Admin duyệt**; trong thời gian này bạn chưa gửi báo giá được.");
            case "REJECTED" -> sb.append("\n\nHồ sơ **bị từ chối**: bạn sửa theo lý do trên tại /tho/ho-so rồi gửi lại để Admin duyệt.");
            default -> { }
        }
        return sb.toString();
    }

    static String jobsText(JsonNode n) {
        JsonNode items = n.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return errorOr(n, n.path("message").asText(
                    "Hiện chưa có yêu cầu mở nào khớp chuyên môn và khu vực của bạn. Bạn quay lại bảng việc /tho sau nhé."));
        }
        StringBuilder sb = new StringBuilder("Các yêu cầu đang mở khớp chuyên môn và khu vực của bạn:\n");
        int i = 1;
        for (JsonNode it : items) {
            if (i > 6) {
                break;
            }
            sb.append(i++).append(". **").append(it.path("title").asText()).append("** (").append(it.path("areaName").asText())
                    .append(") – ngân sách ").append(it.path("budgetText").asText()).append("\n");
        }
        sb.append("\nMở chi tiết từng việc để gửi báo giá (công + vật tư, không vượt ngân sách khách đặt).");
        return sb.toString();
    }

    static String quotesText(String requestCode, JsonNode n) {
        JsonNode items = n.has("quotations") ? n.path("quotations") : n.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return errorOr(n, n.path("message").asText("Đơn **" + requestCode + "** hiện chưa có báo giá nào."));
        }
        return "Đơn **" + requestCode + "** đang có **" + items.size() + "** báo giá (chi tiết trong thẻ bên dưới). "
                + "Bạn so sánh giá, phương án, điểm đánh giá rồi bấm **Chọn thợ này** trong chi tiết đơn; mình không chọn thay bạn.";
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("\n- ").append(label).append(": ").append(value);
        }
    }
}
