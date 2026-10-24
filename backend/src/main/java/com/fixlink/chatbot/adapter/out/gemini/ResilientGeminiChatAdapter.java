package com.fixlink.chatbot.adapter.out.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.adapter.out.llm.ChatReplyParser;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort.ToolExecutionResult;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.service.RepairRequestDraftLink;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gọi Gemini API (generateContent) với chuỗi failover nhiều model và vòng lặp tool calling.
 *
 * <p>Các điểm bắt buộc với dòng Gemini 3.x:
 * <ul>
 *   <li>Khi model trả về {@code functionCall}, phải gửi lại <b>nguyên vẹn</b> các part của
 *       lượt model đó (kèm {@code thoughtSignature}) trong lượt kế tiếp; nếu tự dựng lại part
 *       mà thiếu chữ ký, API trả HTTP 400 "Function call is missing a thought_signature".</li>
 *   <li>Một lượt có thể chứa nhiều {@code functionCall} song song; mỗi lời gọi đều phải có
 *       {@code functionResponse} tương ứng (cùng {@code name} và {@code id}).</li>
 *   <li>Token suy luận (thinking) tính vào {@code maxOutputTokens}, nên giới hạn phải đủ rộng
 *       để câu trả lời JSON không bị cắt cụt.</li>
 * </ul>
 */
@Component("geminiChatAdapter")
public class ResilientGeminiChatAdapter implements LlmChatPort {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientGeminiChatAdapter.class);

    private final ChatbotProperties properties;
    private final ChatbotToolPort toolPort;
    private final ExtractiveFallbackSynthesizer fallbackSynthesizer;
    private final ChatReplyParser replyParser;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, Instant> modelCooldownUntil = new ConcurrentHashMap<>();
    /** Model đã từng từ chối tham số thinkingConfig — không gửi lại cho model đó nữa. */
    private final Set<String> modelsWithoutThinkingConfig = ConcurrentHashMap.newKeySet();
    /** Model không hỗ trợ structured output kèm tool — chỉ dựa vào hướng dẫn JSON trong prompt. */
    private final Set<String> modelsWithoutStructuredOutput = ConcurrentHashMap.newKeySet();

    public ResilientGeminiChatAdapter(
            ChatbotProperties properties,
            ChatbotToolPort toolPort,
            ExtractiveFallbackSynthesizer fallbackSynthesizer,
            ChatReplyParser replyParser,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.toolPort = toolPort;
        this.fallbackSynthesizer = fallbackSynthesizer;
        this.replyParser = replyParser;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public ChatOrchestrationResult generateChatResponse(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            List<Map<String, Object>> tools,
            Long currentUserId,
            String currentRole,
            String currentRoute,
            String guestContact,
            String knowledgeContext
    ) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No GEMINI_API_KEY configured. Using local extractive synthesizer directly.");
            return fallbackSynthesizer.synthesize(userMessage, knowledgeContext);
        }

        Instant start = Instant.now();
        Duration totalBudget = Duration.ofSeconds(properties.getGemini().getTotalBudgetSeconds());
        // Kết quả tool dùng chung cho mọi model trong cùng một lượt hỏi: nếu model đầu đã tạo
        // ticket rồi mới lỗi, model dự phòng sẽ nhận lại đúng kết quả cũ thay vì tạo ticket trùng.
        Map<String, ToolExecutionResult> toolCache = new HashMap<>();
        TicketHolder ticketHolder = new TicketHolder();

        for (String modelName : orderModelsByAvailability()) {
            if (Duration.between(start, Instant.now()).compareTo(totalBudget) > 0) {
                LOG.warn("Chatbot time budget ({}s) exhausted before trying model '{}'", totalBudget.toSeconds(), modelName);
                break;
            }
            try {
                ChatOrchestrationResult result = executeChatWithModel(
                        modelName, apiKey, systemPrompt, history, userMessage, tools,
                        currentUserId, currentRole, guestContact, toolCache, ticketHolder);
                LOG.info("Received answer from Gemini model '{}' in {} ms",
                        modelName, Duration.between(start, Instant.now()).toMillis());
                return result;
            } catch (GeminiCallException ex) {
                LOG.warn("Gemini model '{}' failed: {}. Trying next model...", modelName, ex.getMessage());
                applyCooldown(modelName, ex.status());
            } catch (Exception ex) {
                LOG.warn("Gemini model '{}' failed with unexpected error: {}. Trying next model...",
                        modelName, ex.toString());
                applyCooldown(modelName, 0);
            }
        }

        LOG.error("All Gemini models failed. Activating local knowledge-base synthesizer.");
        ChatOrchestrationResult fallback = fallbackSynthesizer.synthesize(userMessage, knowledgeContext);
        if (ticketHolder.ticketCode != null) {
            return new ChatOrchestrationResult(
                    "Mình đã tạo phiếu hỗ trợ **" + ticketHolder.ticketCode + "** cho bạn. "
                            + "Nhân viên CSKH sẽ liên hệ lại sớm nhất qua thông tin bạn đã cung cấp.",
                    List.of("Xem trạng thái phiếu " + ticketHolder.ticketCode, "Cách đăng yêu cầu sửa chữa"),
                    fallback.actions(),
                    true,
                    ticketHolder.ticketCode,
                    fallback.modelUsed()
            );
        }
        return fallback;
    }

    /** Model đang không bị cooldown đứng trước; nếu tất cả đều cooldown vẫn thử theo thứ tự cấu hình. */
    private List<String> orderModelsByAvailability() {
        List<String> configured = properties.getGemini().getModels();
        Instant now = Instant.now();
        List<String> available = new ArrayList<>();
        for (String model : configured) {
            Instant until = modelCooldownUntil.get(model);
            if (until == null || now.isAfter(until)) {
                available.add(model);
            }
        }
        if (available.isEmpty()) {
            LOG.warn("All Gemini models are in cooldown; retrying them anyway in configured order.");
            return configured;
        }
        return available;
    }

    private void applyCooldown(String modelName, int status) {
        int seconds;
        if (status == 429) {
            seconds = properties.getGemini().getCooldownSeconds();
        } else if (status == 503 || status == 504) {
            // Model đang quá tải/treo phía Google: tạm né đủ lâu để các câu hỏi sau không phải chờ timeout lại
            seconds = 120;
        } else if (status == 500 || status == 0) {
            seconds = 30;
        } else if (status == 404) {
            seconds = 3600; // model không tồn tại với key này
        } else {
            return; // 400 do nội dung yêu cầu: không phải lỗi của model, không phạt
        }
        modelCooldownUntil.put(modelName, Instant.now().plusSeconds(seconds));
    }

    private ChatOrchestrationResult executeChatWithModel(
            String modelName,
            String apiKey,
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            List<Map<String, Object>> tools,
            Long currentUserId,
            String currentRole,
            String guestContact,
            Map<String, ToolExecutionResult> toolCache,
            TicketHolder ticketHolder
    ) throws Exception {
        List<Map<String, Object>> contents = buildContents(history, userMessage);
        List<Map<String, Object>> geminiTools = List.of(Map.of("functionDeclarations", tools));
        int maxRounds = Math.max(1, properties.getLimits().getMaxToolRounds());

        for (int round = 0; round <= maxRounds; round++) {
            // Vòng cuối cùng tắt tool để buộc model chốt câu trả lời thay vì gọi tool mãi.
            boolean forceFinalAnswer = round == maxRounds;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            payload.put("contents", contents);
            payload.put("tools", geminiTools);
            payload.put("toolConfig", Map.of("functionCallingConfig",
                    Map.of("mode", forceFinalAnswer ? "NONE" : "AUTO")));
            payload.put("generationConfig", buildGenerationConfig(modelName));

            JsonNode root = postWithRetry(modelName, apiKey, payload);
            JsonNode candidate = root.path("candidates").path(0);
            if (candidate.isMissingNode()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText("NO_CANDIDATE");
                throw new GeminiCallException(200, "No candidate returned (" + blockReason + ")");
            }

            JsonNode partsNode = candidate.path("content").path("parts");
            List<JsonNode> functionCalls = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            if (partsNode.isArray()) {
                for (JsonNode part : partsNode) {
                    if (part.has("functionCall")) {
                        functionCalls.add(part.get("functionCall"));
                    } else if (part.has("text") && !part.path("thought").asBoolean(false)) {
                        text.append(part.get("text").asText());
                    }
                }
            }

            if (!functionCalls.isEmpty() && !forceFinalAnswer) {
                // Giữ nguyên toàn bộ part của model (gồm thoughtSignature) — bắt buộc với Gemini 3.
                List<Object> modelParts = objectMapper.convertValue(partsNode, new TypeReference<List<Object>>() { });
                contents.add(Map.of("role", "model", "parts", modelParts));

                List<Map<String, Object>> responseParts = new ArrayList<>();
                for (JsonNode call : functionCalls) {
                    responseParts.add(executeToolCall(call, round, currentUserId, currentRole, guestContact,
                            toolCache, ticketHolder));
                }
                contents.add(Map.of("role", "user", "parts", responseParts));
                continue;
            }

            String rawText = text.toString().trim();
            if (rawText.isEmpty()) {
                String finishReason = candidate.path("finishReason").asText("UNKNOWN");
                throw new GeminiCallException(200, "Empty answer (finishReason=" + finishReason + ")");
            }
            return replyParser.withLeadingAction(parseFinalOutput(rawText, ticketHolder.ticketCode, modelName),
                    RepairRequestDraftLink.ACTION_LABEL, ticketHolder.prefillUrl);
        }

        throw new GeminiCallException(200, "Tool loop ended without a final answer");
    }

    private List<Map<String, Object>> buildContents(List<ChatMessage> history, String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                if (msg.getContent() == null || msg.getContent().isBlank()) {
                    continue;
                }
                String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", msg.getContent()))));
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));
        return contents;
    }

    private Map<String, Object> buildGenerationConfig(String modelName) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("temperature", properties.getGemini().getTemperature());
        config.put("maxOutputTokens", properties.getGemini().getMaxOutputTokens());
        String thinkingLevel = properties.getGemini().getThinkingLevel();
        if (thinkingLevel != null && !thinkingLevel.isBlank() && !modelsWithoutThinkingConfig.contains(modelName)) {
            config.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        }
        if (!modelsWithoutStructuredOutput.contains(modelName)) {
            // Structured output: buộc câu trả lời cuối là JSON đúng schema (vẫn cho phép functionCall).
            config.put("responseMimeType", "application/json");
            config.put("responseJsonSchema", RESPONSE_SCHEMA);
        }
        return config;
    }

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "reply", Map.of("type", "string",
                            "description", "Câu trả lời dạng Markdown, trả lời thẳng vào câu hỏi"),
                    "suggestions", Map.of("type", "array", "items", Map.of("type", "string"),
                            "description", "2-4 câu hỏi tiếp theo ngắn gọn, liên quan trực tiếp"),
                    "actions", Map.of("type", "array", "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "type", Map.of("type", "string"),
                                    "label", Map.of("type", "string"),
                                    "url", Map.of("type", "string")),
                            "required", List.of("label", "url"))),
                    "needsHuman", Map.of("type", "boolean")),
            "required", List.of("reply", "suggestions")
    );

    private Map<String, Object> executeToolCall(
            JsonNode call,
            int round,
            Long currentUserId,
            String currentRole,
            String guestContact,
            Map<String, ToolExecutionResult> toolCache,
            TicketHolder ticketHolder
    ) {
        String toolName = call.path("name").asText();
        Map<String, Object> arguments = call.hasNonNull("args")
                ? objectMapper.convertValue(call.get("args"), new TypeReference<Map<String, Object>>() { })
                : new HashMap<>();
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        String cacheKey = toolName + "|" + toJsonQuietly(arguments);
        ToolExecutionResult toolResult = toolCache.get(cacheKey);
        if (toolResult == null) {
            LOG.info("Gemini round {}: calling tool '{}' with args {}", round + 1, toolName, arguments);
            toolResult = toolPort.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            toolCache.put(cacheKey, toolResult);
        } else {
            LOG.info("Gemini round {}: reusing cached result of tool '{}'", round + 1, toolName);
        }

        if (("create_support_ticket".equals(toolName) || "prepare_repair_request".equals(toolName))
                && !toolResult.isError()) {
            try {
                JsonNode tr = objectMapper.readTree(toolResult.content());
                if (tr.hasNonNull("ticketCode")) {
                    ticketHolder.ticketCode = tr.get("ticketCode").asText();
                }
                if (tr.hasNonNull("prefillUrl")) {
                    ticketHolder.prefillUrl = tr.get("prefillUrl").asText();
                }
            } catch (Exception ignored) {
                // nội dung tool không phải JSON — bỏ qua
            }
        }

        Map<String, Object> functionResponse = new LinkedHashMap<>();
        functionResponse.put("name", toolName);
        if (call.hasNonNull("id")) {
            functionResponse.put("id", call.get("id").asText());
        }
        functionResponse.put("response", toResponseObject(toolResult.content()));
        return Map.of("functionResponse", functionResponse);
    }

    /** functionResponse.response phải là một JSON object. */
    private Object toResponseObject(String content) {
        if (content != null) {
            try {
                JsonNode node = objectMapper.readTree(content);
                if (node != null && node.isObject()) {
                    return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() { });
                }
                if (node != null) {
                    return Map.of("result", objectMapper.convertValue(node, Object.class));
                }
            } catch (Exception ignored) {
                // không phải JSON — trả về dạng chuỗi
            }
        }
        return Map.of("result", content != null ? content : "");
    }

    private JsonNode postWithRetry(String modelName, String apiKey, Map<String, Object> payload) throws Exception {
        String url = properties.getGemini().getBaseUrl() + "/models/" + modelName + ":generateContent";
        int attempts = 1 + Math.max(0, properties.getGemini().getMaxRetries());

        for (int attempt = 1; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException ex) {
                throw new GeminiCallException(504, "Timeout after " + properties.getGemini().getTimeoutSeconds() + "s");
            }

            int status = response.statusCode();
            if (status == 200) {
                return objectMapper.readTree(response.body());
            }

            String body = response.body() != null ? response.body() : "";
            if (status == 400 && (body.contains("response_mime_type") || body.contains("responseMimeType")
                    || body.contains("response_json_schema") || body.contains("responseJsonSchema"))
                    && removeGenerationKeys(payload, "responseMimeType", "responseJsonSchema")) {
                LOG.info("Model '{}' rejected structured output with tools; retrying with plain JSON instructions.", modelName);
                modelsWithoutStructuredOutput.add(modelName);
                attempt--;
                continue;
            }
            if (status == 400 && body.contains("thinking") && removeGenerationKeys(payload, "thinkingConfig")) {
                LOG.info("Model '{}' rejected thinkingConfig; retrying without it.", modelName);
                modelsWithoutThinkingConfig.add(modelName);
                attempt--; // lần thử lại này không tính vào số lần retry
                continue;
            }

            boolean transientError = status == 503 || status == 500 || status == 504
                    || (status == 429 && !body.contains("PerDay"));
            if (transientError && attempt < attempts) {
                long backoffMs = 1200L * attempt;
                LOG.info("Model '{}' returned HTTP {} (attempt {}/{}); retrying in {} ms",
                        modelName, status, attempt, attempts, backoffMs);
                Thread.sleep(backoffMs);
                continue;
            }
            throw new GeminiCallException(status, "HTTP " + status + ": " + abbreviate(body, 300));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean removeGenerationKeys(Map<String, Object> payload, String... keys) {
        Object gc = payload.get("generationConfig");
        if (!(gc instanceof Map<?, ?> map)) {
            return false;
        }
        Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) map);
        boolean removed = false;
        for (String key : keys) {
            removed |= copy.remove(key) != null;
        }
        payload.put("generationConfig", copy);
        return removed;
    }

    ChatOrchestrationResult parseFinalOutput(String rawText, String ticketId, String modelName) {
        try {
            return replyParser.parse(rawText, ticketId, modelName);
        } catch (IllegalStateException ex) {
            throw new GeminiCallException(200, ex.getMessage());
        }
    }

    public String sanitizeOutputLinks(String text) {
        return replyParser.sanitizeOutputLinks(text);
    }

    private String toJsonQuietly(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static final class TicketHolder {
        private String ticketCode;
        private String prefillUrl;
    }

    /** Lỗi khi gọi một model cụ thể; {@code status} là HTTP status (0 nếu không xác định). */
    static final class GeminiCallException extends RuntimeException {
        private final int status;

        GeminiCallException(int status, String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
