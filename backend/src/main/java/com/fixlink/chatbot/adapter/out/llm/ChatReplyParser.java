package com.fixlink.chatbot.adapter.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Chuyển câu trả lời cuối của LLM (JSON {reply, suggestions, actions, needsHuman} hoặc văn bản thường)
 * thành {@link ChatOrchestrationResult}, đồng thời chặn link ra ngoài FixLink. Dùng chung cho mọi
 * nhà cung cấp LLM (Groq, Gemini).
 */
@Component
public class ChatReplyParser {

    private static final Set<String> ALLOWED_ROUTE_PREFIXES = Set.of(
            "/",
            "/dang-yeu-cau",
            "/yeu-cau-cua-toi",
            "/yeu-cau",
            "/dang-nhap",
            "/dang-nhap-tho",
            "/login",
            "/dang-ky",
            "/register",
            "/dang-ky-tho",
            "/quen-mat-khau",
            "/dat-lai-mat-khau",
            "/khach-hang",
            "/tho",
            "/tho/ho-so",
            "/tho/yeu-cau"
    );

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Cách đăng yêu cầu sửa chữa",
            "Chính sách đặt cọc thế nào?",
            "Gặp nhân viên hỗ trợ"
    );

    private static final int MAX_SUGGESTIONS = 4;
    private static final int MAX_SUGGESTION_LENGTH = 90;

    private final ObjectMapper objectMapper;

    public ChatReplyParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @throws IllegalStateException khi model trả về câu trả lời rỗng (để adapter thử model khác)
     */
    public ChatOrchestrationResult parse(String rawText, String ticketId, String modelName) {
        String cleanJson = stripCodeFence(rawText);
        int startBrace = cleanJson.indexOf('{');
        int endBrace = cleanJson.lastIndexOf('}');
        if (startBrace != -1 && endBrace > startBrace) {
            cleanJson = cleanJson.substring(startBrace, endBrace + 1);
        }

        String reply;
        List<String> suggestions = new ArrayList<>();
        List<Map<String, String>> actions = new ArrayList<>();
        boolean needsHuman = false;

        try {
            JsonNode root = objectMapper.readTree(cleanJson);
            if (root == null || !root.isObject() || !root.hasNonNull("reply")) {
                throw new IllegalArgumentException("Missing reply field");
            }
            reply = root.get("reply").asText();

            if (root.path("suggestions").isArray()) {
                for (JsonNode s : root.get("suggestions")) {
                    addSuggestion(suggestions, s.asText());
                }
            }
            if (root.path("actions").isArray()) {
                for (JsonNode a : root.get("actions")) {
                    String url = a.path("url").asText("").trim();
                    if (isValidInternalUrl(url) && actions.size() < 3) {
                        actions.add(Map.of(
                                "type", a.path("type").asText("link"),
                                "label", a.path("label").asText("Xem thêm"),
                                "url", url));
                    }
                }
            }
            needsHuman = root.path("needsHuman").asBoolean(false);
        } catch (Exception ex) {
            // Model trả lời dạng văn bản thường (không phải JSON) — vẫn là câu trả lời hợp lệ.
            reply = rescueReplyText(cleanJson, rawText);
        }

        reply = sanitizeOutputLinks(reply).trim();
        if (reply.isEmpty()) {
            throw new IllegalStateException("Model returned an empty reply field");
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(DEFAULT_SUGGESTIONS);
        }
        return new ChatOrchestrationResult(reply, suggestions, actions, needsHuman || ticketId != null,
                ticketId, modelName);
    }

    /** Đặt nút hành động do hệ thống tạo (ví dụ form điền sẵn) lên đầu danh sách actions, không trùng lặp. */
    public ChatOrchestrationResult withLeadingAction(ChatOrchestrationResult result, String label, String url) {
        if (result == null || url == null || !isValidInternalUrl(url)) {
            return result;
        }
        List<Map<String, String>> actions = new ArrayList<>();
        actions.add(Map.of("type", "link", "label", label, "url", url));
        if (result.actions() != null) {
            for (Map<String, String> a : result.actions()) {
                String existing = a.get("url");
                if (existing != null && !existing.startsWith("/dang-yeu-cau") && actions.size() < 3) {
                    actions.add(a);
                }
            }
        }
        return new ChatOrchestrationResult(result.reply(), result.suggestions(), actions, result.needsHuman(),
                result.ticketId(), result.modelUsed());
    }

    public String sanitizeOutputLinks(String text) {
        if (text == null) {
            return "";
        }
        // [label](http://...) -> label : không cho chatbot dẫn người dùng ra ngoài FixLink
        return text.replaceAll("\\[([^\\]]+)\\]\\((https?:\\/\\/[^\\)]+)\\)", "$1");
    }

    boolean isValidInternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String clean = url.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("//")
                || clean.contains("{") || clean.contains(" ")) {
            return false; // không cho link ngoài hoặc route còn placeholder chưa thay
        }
        for (String prefix : ALLOWED_ROUTE_PREFIXES) {
            if (clean.equals(prefix) || clean.startsWith(prefix + "/") || clean.startsWith(prefix + "?")) {
                return true;
            }
        }
        return false;
    }

    private void addSuggestion(List<String> suggestions, String raw) {
        if (raw == null) {
            return;
        }
        String s = raw.trim();
        if (s.isEmpty() || suggestions.size() >= MAX_SUGGESTIONS || suggestions.contains(s)) {
            return;
        }
        suggestions.add(s.length() > MAX_SUGGESTION_LENGTH ? s.substring(0, MAX_SUGGESTION_LENGTH) : s);
    }

    private String stripCodeFence(String text) {
        String t = text.trim();
        Matcher m = Pattern.compile("```(?:json|JSON)?\\s*([\\s\\S]*?)```").matcher(t);
        if (m.find()) {
            return m.group(1).trim();
        }
        if (t.startsWith("```")) {
            return t.replaceFirst("^```(?:json|JSON)?", "").trim();
        }
        return t;
    }

    private String rescueReplyText(String cleanJson, String rawText) {
        try {
            Matcher m = Pattern.compile("\"reply\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(cleanJson);
            if (m.find()) {
                return unescapeJsonString(m.group(1));
            }
            Matcher unterminated = Pattern.compile("\"reply\"\\s*:\\s*\"([\\s\\S]*)").matcher(cleanJson);
            if (unterminated.find()) {
                String partial = unterminated.group(1).trim();
                int cut = partial.indexOf("\",");
                if (cut > 0) {
                    partial = partial.substring(0, cut);
                }
                if (partial.endsWith("\"")) {
                    partial = partial.substring(0, partial.length() - 1);
                }
                return unescapeJsonString(partial);
            }
        } catch (Exception ignored) {
            // ignore and fallback below
        }
        return rawText.replaceAll("(?s)```(?:json|JSON)?\\s*", "").replace("```", "").trim();
    }

    private String unescapeJsonString(String s) {
        try {
            return objectMapper.readValue("\"" + s + "\"", String.class);
        } catch (Exception e) {
            return s.replace("\\n", "\n").replace("\\\"", "\"");
        }
    }
}
