package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.application.service.ToolCallRecorder.ToolTrace;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bộ nhớ đệm câu trả lời cho câu hỏi phổ biến ở lượt đầu của phiên (ví dụ "FixLink là gì?", "cọc bao nhiêu").
 * Giúp trả lời tức thì và tiết kiệm hạn mức token/phút của LLM khi nhiều người hỏi cùng một câu.
 *
 * <p>Chỉ lưu khi: phiên chưa có lịch sử, câu trả lời do LLM thật tạo (không phải dự phòng cục bộ),
 * không tạo phiếu hỗ trợ và không dùng tool đọc dữ liệu cá nhân — nên không bao giờ trả dữ liệu
 * của người này cho người khác.
 */
@Component
public class FaqAnswerCache {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_ENTRIES = 300;

    /** Tool chỉ đọc dữ liệu công khai (danh mục, giá, khu vực, kho tri thức...). */
    private static final Set<String> PUBLIC_TOOLS = Set.of(
            "list_service_categories", "list_services", "list_service_areas", "suggest_category",
            "calculate_deposit", "search_knowledge_base");

    public record Entry(ChatOrchestrationResult result, List<ToolTrace> traces) { }

    private record Stored(Entry entry, Instant expiresAt) { }

    private final Map<String, Stored> store = new ConcurrentHashMap<>();

    public Entry lookup(String message, String role, List<ChatMessage> history) {
        String key = keyFor(message, role, history);
        if (key == null) {
            return null;
        }
        Stored stored = store.get(key);
        if (stored == null) {
            return null;
        }
        if (Instant.now().isAfter(stored.expiresAt())) {
            store.remove(key);
            return null;
        }
        ChatOrchestrationResult r = stored.entry().result();
        String model = r.modelUsed() != null && !r.modelUsed().endsWith("(cache)") ? r.modelUsed() + " (cache)" : r.modelUsed();
        return new Entry(new ChatOrchestrationResult(r.reply(), r.suggestions(), r.actions(), r.needsHuman(), null, model),
                stored.entry().traces());
    }

    public void store(String message, String role, List<ChatMessage> history,
                      ChatOrchestrationResult result, List<ToolTrace> traces) {
        String key = keyFor(message, role, history);
        if (key == null || result == null || result.ticketId() != null) {
            return;
        }
        String model = result.modelUsed() != null ? result.modelUsed() : "";
        if (model.startsWith("local-") || model.isBlank()) {
            return;
        }
        boolean onlyPublicTools = traces == null || traces.stream().allMatch(t -> PUBLIC_TOOLS.contains(t.toolName()));
        if (!onlyPublicTools) {
            return;
        }
        if (store.size() >= MAX_ENTRIES) {
            Instant now = Instant.now();
            store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
            if (store.size() >= MAX_ENTRIES) {
                store.clear();
            }
        }
        store.put(key, new Stored(new Entry(result, traces != null ? List.copyOf(traces) : List.of()),
                Instant.now().plus(TTL)));
    }

    /** Chỉ cache lượt đầu của phiên; câu hỏi giống nhau sau khi bỏ dấu, viết tắt, dấu câu dùng chung khóa. */
    String keyFor(String message, String role, List<ChatMessage> history) {
        if (message == null || (history != null && !history.isEmpty())) {
            return null;
        }
        String normalized = SymptomClassifier.normalize(message);
        if (normalized.isBlank() || normalized.length() > 200) {
            return null;
        }
        String roleGroup = role != null ? role.toUpperCase() : "GUEST";
        return roleGroup + "|" + normalized;
    }

    void clear() {
        store.clear();
    }
}
