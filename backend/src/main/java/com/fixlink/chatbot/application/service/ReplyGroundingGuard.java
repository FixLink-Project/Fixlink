package com.fixlink.chatbot.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lớp chặn cuối chống bịa thông tin: LLM đôi khi tự nghĩ ra số hotline, email, website hay tỷ lệ phần trăm
 * mà FixLink chưa công bố. Mọi thông tin dạng này không xuất hiện trong hội thoại của chính khách hàng
 * sẽ bị thay bằng câu nói rõ là chưa công bố.
 */
@Component
public class ReplyGroundingGuard {

    private static final Logger LOG = LoggerFactory.getLogger(ReplyGroundingGuard.class);

    /** Tỷ lệ có thật trong quy định FixLink: cọc 30%, tất toán 70%. */
    private static final Set<String> KNOWN_PERCENTAGES = Set.of("10", "30", "70", "90", "100");

    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\d.])(?:\\+?84|0)(?:[\\s.-]?\\d){8,10}(?![\\d])|(?<!\\d)1[89]00(?:[\\s.-]?\\d){4,6}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern WEBSITE = Pattern.compile(
            "(?i)\\b(?:https?://\\S+|www\\.\\S+|[a-z0-9-]+\\.(?:vn|com|net|org|io)(?:/\\S*)?)");
    private static final Pattern PERCENT = Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:[.,]\\d+)?)\\s?%");

    /**
     * @param conversationText toàn bộ nội dung người dùng đã gửi trong phiên (tin hiện tại + lịch sử);
     *                         thông tin khách tự cung cấp (ví dụ SĐT của khách) được giữ nguyên
     */
    public String enforce(String reply, String conversationText) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }
        String context = conversationText != null ? conversationText : "";
        String digitsInContext = context.replaceAll("\\D", " ");

        String result = replaceUnless(PHONE, reply, m -> {
            String digits = m.group().replaceAll("\\D", "");
            return digitsInContext.contains(digits) || context.replaceAll("\\D", "").contains(digits);
        }, "(FixLink chưa công bố số điện thoại liên hệ)");

        result = replaceUnless(EMAIL, result, m -> context.toLowerCase().contains(m.group().toLowerCase()),
                "(FixLink chưa công bố email liên hệ)");

        result = replaceUnless(WEBSITE, result, m -> context.toLowerCase().contains(m.group().toLowerCase()),
                "website FixLink");

        result = replaceUnless(PERCENT, result, m -> {
            String value = m.group(1).replace(',', '.');
            String integer = value.contains(".") ? value.substring(0, value.indexOf('.')) : value;
            return KNOWN_PERCENTAGES.contains(integer) || context.contains(m.group(1) + "%")
                    || context.contains(m.group(1) + " %");
        }, "(tỷ lệ này FixLink chưa công bố)");

        if (!result.equals(reply)) {
            LOG.warn("Grounding guard removed unpublished contact info / figures from chatbot reply");
        }
        return humanizeInternalCodes(result);
    }

    /** Mã nội bộ (trạng thái, danh mục) → tên tiếng Việt khách nhìn thấy trên website. */
    private static final Map<String, String> CODE_LABELS = Map.ofEntries(
            Map.entry("AWAITING_COST_APPROVAL", "Chờ duyệt chi phí phát sinh"),
            Map.entry("AWAITING_ACCEPTANCE", "Chờ nghiệm thu"),
            Map.entry("MATCHED_AWAITING_DEPOSIT", "Đã chọn thợ"),
            Map.entry("PENDING_APPROVAL", "Chờ xác nhận"),
            Map.entry("DEPOSIT_PAID", "Đã đặt cọc"),
            Map.entry("BIDDING_OPEN", "Đang nhận báo giá"),
            Map.entry("IN_PROGRESS", "Đang sửa chữa"),
            Map.entry("INSPECTING", "Đang khảo sát"),
            Map.entry("RESCHEDULED", "Đã dời lịch"),
            Map.entry("ASSIGNED", "Đã chọn thợ"),
            Map.entry("COMPLETED", "Đã hoàn thành"),
            Map.entry("CANCELLED", "Đã hủy"),
            Map.entry("CONFIRMED", "Đã xác nhận"),
            Map.entry("APPROVED", "Đã duyệt"),
            Map.entry("REJECTED", "Bị từ chối"),
            Map.entry("PENDING", "Chờ duyệt"),
            Map.entry("DRAFT", "Bản nháp"),
            Map.entry("OPEN", "Đang nhận báo giá"),
            Map.entry("DIEN_LANH", "Sửa Chữa Điện Lạnh"),
            Map.entry("DIEN_NUOC", "Sửa Chữa Điện Nước"),
            Map.entry("DO_GIA_DUNG", "Sửa Điện Gia Dụng"),
            Map.entry("KHOA_CUA", "Sửa Khóa & Cửa Cuốn"),
            Map.entry("BINH_THANH", "Bình Thạnh"),
            Map.entry("THU_DUC", "TP. Thủ Đức"),
            Map.entry("TAN_BINH", "Tân Bình"),
            Map.entry("GO_VAP", "Gò Vấp"),
            Map.entry("PHU_NHUAN", "Phú Nhuận"));

    private static final String CODE_ALTERNATION = String.join("|", CODE_LABELS.keySet().stream()
            .sorted((a, b) -> b.length() - a.length()).toList());
    /** "Đang sửa chữa (IN_PROGRESS)": tên tiếng Việt đã có, bỏ phần mã trong ngoặc. */
    private static final Pattern PARENTHESIZED_CODE = Pattern.compile(
            "\\s*\\(\\s*`?(?:" + CODE_ALTERNATION + ")`?\\s*\\)");
    /** Mã đứng riêng (không nằm trong mã đơn như REQ-TEST-ASSIGNED). */
    private static final Pattern BARE_CODE = Pattern.compile(
            "`?(?<![A-Za-z0-9_-])(" + CODE_ALTERNATION + ")(?![A-Za-z0-9_-])`?");
    /** Tên công cụ nội bộ không được lộ ra câu trả lời. */
    private static final Pattern TOOL_NAME = Pattern.compile(
            "`?\\b(?:get_my_repair_requests|get_repair_request_status|get_request_quotations|get_my_technician_status|"
                    + "find_matching_jobs|get_my_appointments|create_support_ticket|read_support_ticket|"
                    + "search_knowledge_base|list_service_categories|list_services|list_service_areas|"
                    + "suggest_category|calculate_deposit|prepare_repair_request)\\b`?");

    /**
     * Bảng markdown hiển thị vỡ trong khung chat hẹp (nhất là trên điện thoại): đổi mỗi hàng thành một gạch đầu dòng
     * "ô 1 – ô 2 · ô 3...", bỏ hàng tiêu đề và hàng phân cách.
     */
    static String tablesToBullets(String text) {
        if (!text.contains("|")) {
            return text;
        }
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean inTable = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            boolean tableRow = line.startsWith("|") && line.endsWith("|") && line.length() > 2;
            if (!tableRow) {
                inTable = false;
                sb.append(lines[i]).append(i < lines.length - 1 ? "\n" : "");
                continue;
            }
            boolean nextIsSeparator = i + 1 < lines.length && lines[i + 1].trim().matches("\\|?\\s*:?-{2,}.*");
            if (line.matches("\\|?\\s*:?-{2,}.*") || (!inTable && nextIsSeparator)) {
                inTable = true; // hàng phân cách hoặc hàng tiêu đề
                continue;
            }
            inTable = true;
            List<String> cells = new ArrayList<>();
            for (String cell : line.substring(1, line.length() - 1).split("\\|")) {
                if (!cell.isBlank()) {
                    cells.add(cell.trim());
                }
            }
            if (!cells.isEmpty()) {
                sb.append("- ").append(cells.get(0));
                if (cells.size() > 1) {
                    sb.append(" – ").append(String.join(" · ", cells.subList(1, cells.size())));
                }
                sb.append(i < lines.length - 1 ? "\n" : "");
            }
        }
        return sb.toString();
    }

    static String humanizeInternalCodes(String text) {
        String result = PARENTHESIZED_CODE.matcher(tablesToBullets(text)).replaceAll("");
        Matcher m = BARE_CODE.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(CODE_LABELS.get(m.group(1))));
        }
        m.appendTail(sb);
        return TOOL_NAME.matcher(sb.toString()).replaceAll("hệ thống");
    }

    private interface Allow {
        boolean test(Matcher m);
    }

    private static String replaceUnless(Pattern pattern, String text, Allow allow, String replacement) {
        Matcher m = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(allow.test(m) ? m.group() : replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
