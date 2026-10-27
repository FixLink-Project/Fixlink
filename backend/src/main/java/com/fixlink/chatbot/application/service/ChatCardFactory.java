package com.fixlink.chatbot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.service.ToolCallRecorder.ToolTrace;
import com.fixlink.chatbot.domain.model.ChatCard;
import com.fixlink.chatbot.domain.model.ChatCard.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Dựng thẻ thông tin trực quan từ kết quả tool của lượt hỏi (giống thẻ tra cứu đơn hàng của các sàn
 * thương mại điện tử). Thẻ chỉ dùng dữ liệu thật trả về từ hệ thống.
 */
@Component
public class ChatCardFactory {

    private static final int MAX_CARDS = 4;

    static final List<String> REQUEST_STEPS = List.of(
            "Nhận báo giá", "Đã chọn thợ", "Đặt cọc", "Khảo sát", "Sửa chữa", "Chờ nghiệm thu", "Hoàn thành");

    private final ObjectMapper objectMapper;

    public ChatCardFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ChatCard> fromToolTraces(List<ToolTrace> traces) {
        List<ChatCard> cards = new ArrayList<>();
        Set<String> seenRequests = new LinkedHashSet<>();
        if (traces == null) {
            return cards;
        }
        // Thẻ chi tiết một đơn ưu tiên hơn thẻ trong danh sách đơn
        for (ToolTrace trace : traces) {
            if ("get_repair_request_status".equals(trace.toolName()) && !trace.error()) {
                JsonNode node = read(trace.resultJson());
                if (node != null && (node.has("requestId") || node.has("id"))) {
                    cards.add(requestCard(node, true, null));
                    seenRequests.add(node.has("requestId") ? node.path("requestId").asText() : node.path("id").asText());
                }
            }
        }
        for (ToolTrace trace : traces) {
            if (trace.error() || cards.size() >= MAX_CARDS) {
                continue;
            }
            JsonNode node = read(trace.resultJson());
            if (node == null || node.has("requireLogin")) {
                continue;
            }
            switch (trace.toolName()) {
                case "get_my_repair_requests" -> {
                    for (JsonNode item : node.path("items")) {
                        if (cards.size() >= MAX_CARDS) {
                            break;
                        }
                        if (seenRequests.add(item.path("id").asText())) {
                            cards.add(requestCard(item, false, node.path("viewerRole").asText("")));
                        }
                    }
                }
                case "get_my_appointments" -> {
                    for (JsonNode item : node.path("appointments")) {
                        if (cards.size() >= MAX_CARDS) {
                            break;
                        }
                        cards.add(appointmentCard(item));
                    }
                }
                case "list_services" -> cards.add(priceListCard(node));
                case "calculate_deposit" -> cards.add(depositCard(node));
                case "get_my_technician_status" -> cards.add(technicianCard(node));
                case "find_matching_jobs" -> {
                    for (JsonNode item : node.path("items")) {
                        if (cards.size() >= MAX_CARDS) {
                            break;
                        }
                        cards.add(jobCard(item));
                    }
                }
                case "prepare_repair_request" -> cards.add(draftCard(node));
                case "get_request_quotations" -> {
                    if (node.path("totalQuotations").asInt(0) > 0) {
                        cards.add(quotationsCard(node));
                    }
                }
                default -> {
                    // các tool khác không cần thẻ
                }
            }
        }
        return cards.size() > MAX_CARDS ? cards.subList(0, MAX_CARDS) : cards;
    }

    ChatCard requestCard(JsonNode r, boolean detailed, String viewerRole) {
        String status = r.path("status").asText("");
        List<Field> fields = new ArrayList<>();
        addField(fields, "Mã đơn", r.path("requestCode").asText(""));
        addField(fields, "Danh mục", r.path("categoryName").asText(""));
        if (r.has("technicianName")) {
            addField(fields, "Thợ phụ trách", r.path("technicianName").asText(""));
        }
        if (r.has("customerName")) {
            addField(fields, "Khách hàng", r.path("customerName").asText(""));
        }
        addField(fields, r.has("agreedPriceText") ? "Giá chốt" : "Ngân sách",
                r.has("agreedPriceText") ? r.path("agreedPriceText").asText("") : r.path("budgetText").asText(""));
        if (r.has("depositText")) {
            addField(fields, "Tiền cọc (30%)", r.path("depositText").asText(""));
        }
        if (r.has("applyDeadline")) {
            addField(fields, "Hạn nhận báo giá", r.path("applyDeadline").asText(""));
        }
        if (detailed) {
            // Ưu tiên lịch hẹn còn hiệu lực (chưa hủy/chưa hoàn thành); không có thì lấy lịch gần nhất
            JsonNode chosen = null;
            for (JsonNode a : r.path("appointments")) {
                String st = a.path("status").asText("");
                if (!"CANCELLED".equals(st) && !"COMPLETED".equals(st)) {
                    chosen = a;
                    break;
                }
                if (chosen == null) {
                    chosen = a;
                }
            }
            if (chosen != null) {
                addField(fields, "Lịch hẹn", formatSchedule(chosen) + " · " + chosen.path("statusLabel").asText(""));
            }
        }

        Integer step = stepOf(status);
        String id = r.has("requestId") ? r.path("requestId").asText() : r.path("id").asText();
        String url = r.has("detailUrl") ? r.path("detailUrl").asText()
                : ("TECHNICIAN".equals(viewerRole) ? "/tho/yeu-cau/" : "/yeu-cau-cua-toi/") + id;
        return ChatCard.builder()
                .type("request")
                .title(r.path("title").asText("Yêu cầu sửa chữa"))
                .badge(r.path("statusLabel").asText(status))
                .badgeTone(toneOf(status))
                .fields(fields)
                .steps(step != null ? REQUEST_STEPS : null)
                .currentStep(step)
                .note(detailed ? r.path("nextStep").asText(null) : null)
                .url(url)
                .urlLabel("Xem chi tiết đơn")
                .build();
    }

    /** "15:00:00" + "2026-10-21" → "15:00 21/10/2026". */
    static String formatSchedule(JsonNode a) {
        String time = a.path("scheduledTime").asText("");
        String date = a.path("scheduledDate").asText("");
        if (time.length() >= 5) {
            time = time.substring(0, 5);
        }
        if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
        }
        return (time + " " + date).trim();
    }

    private ChatCard appointmentCard(JsonNode a) {
        List<Field> fields = new ArrayList<>();
        addField(fields, "Thời gian", formatSchedule(a));
        addField(fields, "Đơn", a.path("requestCode").asText(""));
        addField(fields, "Thợ", a.path("technicianName").asText(""));
        addField(fields, "Loại hẹn", a.path("appointmentType").asText(""));
        if (a.has("cancelReason")) {
            addField(fields, "Lý do hủy", a.path("cancelReason").asText(""));
        }
        String status = a.path("status").asText("");
        return ChatCard.builder()
                .type("appointment")
                .title(a.path("requestTitle").asText("Lịch hẹn"))
                .badge(a.path("statusLabel").asText(status))
                .badgeTone(switch (status) {
                    case "COMPLETED" -> "success";
                    case "CANCELLED" -> "danger";
                    case "RESCHEDULED" -> "warning";
                    default -> "info";
                })
                .fields(fields)
                .url("/yeu-cau-cua-toi")
                .urlLabel("Quản lý lịch hẹn")
                .build();
    }

    private ChatCard priceListCard(JsonNode node) {
        List<Field> fields = new ArrayList<>();
        for (JsonNode s : node.path("services")) {
            if (fields.size() >= 8) {
                break;
            }
            String duration = s.hasNonNull("estimatedDurationMinutes")
                    ? " · ~" + s.path("estimatedDurationMinutes").asInt() + " phút" : "";
            addField(fields, s.path("name").asText(""), s.path("basePriceText").asText("") + duration);
        }
        return ChatCard.builder()
                .type("price_list")
                .title("Bảng giá tham khảo")
                .subtitle("Giá khởi điểm theo gói dịch vụ; giá thực tế là ngân sách bạn tự đặt")
                .fields(fields)
                .url("/dang-yeu-cau")
                .urlLabel("Đăng yêu cầu")
                .build();
    }

    private ChatCard depositCard(JsonNode node) {
        List<Field> fields = new ArrayList<>();
        addField(fields, "Giá chốt", node.path("amountText").asText(""));
        addField(fields, "Đặt cọc qua QR (30%)", node.path("depositText").asText(""));
        addField(fields, "Thanh toán khi nghiệm thu (70%)", node.path("remainingText").asText(""));
        return ChatCard.builder()
                .type("deposit")
                .title("Tạm tính tiền cọc")
                .fields(fields)
                .note("Cọc trả qua mã QR sau khi bạn xác nhận lịch hẹn và được FixLink giữ bảo chứng. Có phát sinh đã duyệt thì cộng vào lần thanh toán cuối; thợ chỉ nhận tiền khi bạn nghiệm thu.")
                .build();
    }

    private ChatCard technicianCard(JsonNode node) {
        String status = node.path("verificationStatus").asText("PENDING");
        List<Field> fields = new ArrayList<>();
        addField(fields, "Trạng thái KYC", node.path("verificationLabel").asText(""));
        if (node.has("rejectionReason")) {
            addField(fields, "Lý do từ chối", node.path("rejectionReason").asText(""));
        }
        addField(fields, "Trực tuyến", node.path("isOnline").asBoolean() ? "Đang bật" : "Đang tắt");
        addField(fields, "Việc đã hoàn thành", node.path("completedJobs").asText("0"));
        addField(fields, "Điểm đánh giá", node.path("avgRating").asText(""));
        return ChatCard.builder()
                .type("technician")
                .title(node.path("fullName").asText("Hồ sơ thợ"))
                .badge(switch (status) {
                    case "APPROVED" -> "Đã xác minh";
                    case "REJECTED" -> "Bị từ chối";
                    default -> "Chờ duyệt";
                })
                .badgeTone(switch (status) {
                    case "APPROVED" -> "success";
                    case "REJECTED" -> "danger";
                    default -> "warning";
                })
                .fields(fields)
                .url(node.path("profileUrl").asText("/tho/ho-so"))
                .urlLabel("Mở hồ sơ thợ")
                .build();
    }

    private ChatCard jobCard(JsonNode j) {
        List<Field> fields = new ArrayList<>();
        addField(fields, "Mã đơn", j.path("requestCode").asText(""));
        addField(fields, "Danh mục", j.path("categoryName").asText(""));
        addField(fields, "Khu vực", j.path("areaName").asText(""));
        addField(fields, "Ngân sách", j.path("budgetText").asText(""));
        addField(fields, "Hạn báo giá", j.path("applyDeadline").asText(""));
        return ChatCard.builder()
                .type("job")
                .title(j.path("title").asText("Việc đang mở"))
                .badge("Đang tìm thợ")
                .badgeTone("info")
                .fields(fields)
                .url(j.path("detailUrl").asText("/tho"))
                .urlLabel("Xem và gửi báo giá")
                .build();
    }

    private ChatCard quotationsCard(JsonNode node) {
        List<Field> fields = new ArrayList<>();
        for (JsonNode q : node.path("quotations")) {
            if (fields.size() >= 6) {
                break;
            }
            StringBuilder label = new StringBuilder(q.path("technicianName").asText("Thợ"));
            if (q.path("isMine").asBoolean(false)) {
                label.append(" (bạn)");
            }
            if (q.hasNonNull("avgRating")) {
                label.append(" · ★").append(q.path("avgRating").asText());
            }
            if (q.hasNonNull("completedJobs")) {
                label.append(" · ").append(q.path("completedJobs").asInt()).append(" việc");
            }
            addField(fields, label.toString(), q.path("totalPriceText").asText("") + " · " + q.path("statusLabel").asText(""));
        }
        boolean technician = "TECHNICIAN".equals(node.path("viewerRole").asText());
        return ChatCard.builder()
                .type("quotes")
                .title(technician ? "Sàn báo giá (đối thủ ẩn danh)" : "So sánh báo giá")
                .subtitle(node.path("pendingQuotations").asInt(0) + " báo giá đang chờ"
                        + (node.hasNonNull("lowestPendingPriceText") ? " · thấp nhất " + node.path("lowestPendingPriceText").asText() : ""))
                .fields(fields)
                .note(technician ? null : "Bạn tự chọn thợ trong chi tiết đơn – nên cân nhắc cả giá, phương án và điểm đánh giá.")
                .url(node.path("detailUrl").asText(null))
                .urlLabel(technician ? "Mở chi tiết để báo giá" : "Xem chi tiết và chọn thợ")
                .build();
    }

    private ChatCard draftCard(JsonNode node) {
        List<Field> fields = new ArrayList<>();
        addField(fields, "Danh mục", node.path("categoryName").asText(""));
        addField(fields, "Tiêu đề", node.path("title").asText(""));
        addField(fields, "Khu vực", node.path("areaName").asText(""));
        addField(fields, "Ngân sách", node.path("budgetText").asText(""));
        List<String> needed = new ArrayList<>();
        node.path("stillNeeded").forEach(n -> needed.add(n.asText()));
        return ChatCard.builder()
                .type("draft")
                .title("Bản nháp yêu cầu sửa chữa")
                .fields(fields)
                .note(needed.isEmpty() ? null : "Cần bổ sung: " + String.join(", ", needed))
                .url(node.path("prefillUrl").asText("/dang-yeu-cau"))
                .urlLabel("Mở form đã điền sẵn")
                .build();
    }

    static Integer stepOf(String status) {
        return switch (status) {
            case "DRAFT", "PENDING", "OPEN", "BIDDING_OPEN", "MATCHED_AWAITING_DEPOSIT" -> 0;
            case "ASSIGNED" -> 1;
            case "DEPOSIT_PAID" -> 2;
            case "INSPECTING", "AWAITING_COST_APPROVAL" -> 3;
            case "IN_PROGRESS" -> 4;
            case "AWAITING_ACCEPTANCE" -> 5;
            case "COMPLETED" -> 6;
            default -> null; // CANCELLED: không hiển thị thanh tiến độ
        };
    }

    static String toneOf(String status) {
        return switch (status) {
            case "COMPLETED" -> "success";
            case "CANCELLED" -> "danger";
            case "ASSIGNED", "AWAITING_COST_APPROVAL", "AWAITING_ACCEPTANCE" -> "warning"; // khách cần thao tác
            case "DRAFT" -> "neutral";
            default -> "info";
        };
    }

    private static void addField(List<Field> fields, String label, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(new Field(label, value));
        }
    }

    private JsonNode read(String json) {
        try {
            return json != null ? objectMapper.readTree(json) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
