package com.fixlink.chatbot.adapter.out.groq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fixlink.chatbot.adapter.out.gemini.ExtractiveFallbackSynthesizer;
import com.fixlink.chatbot.adapter.out.llm.ChatReplyParser;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort.ToolExecutionResult;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.service.RepairRequestDraftLink;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties.GroqProperties;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gọi Groq qua endpoint tương thích OpenAI ({@code /chat/completions}) với chuỗi failover nhiều
 * model và vòng lặp tool calling.
 *
 * <p>Khai báo tool của hệ thống viết theo kiểu Gemini (type viết hoa: OBJECT, STRING...), adapter
 * chuyển sang JSON Schema chuẩn OpenAI trước khi gửi.
 */
@Component("groqChatAdapter")
public class GroqChatAdapter implements LlmChatPort {

    private static final Logger LOG = LoggerFactory.getLogger(GroqChatAdapter.class);

    static final String PREFILL_ACTION_LABEL = RepairRequestDraftLink.ACTION_LABEL;

    private final ChatbotProperties properties;
    private final ChatbotToolPort toolPort;
    private final ExtractiveFallbackSynthesizer fallbackSynthesizer;
    private final ChatReplyParser replyParser;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, Instant> modelCooldownUntil = new ConcurrentHashMap<>();

    public GroqChatAdapter(
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

    private GroqProperties groq() {
        return properties.getGroq();
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
        String apiKey = groq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No GROQ_API_KEY configured. Using local knowledge-base synthesizer directly.");
            return fallbackSynthesizer.synthesize(userMessage, knowledgeContext);
        }

        Instant start = Instant.now();
        Duration totalBudget = Duration.ofSeconds(groq().getTotalBudgetSeconds());
        List<Map<String, Object>> openAiTools = toOpenAiTools(tools);
        // Dùng chung kết quả tool giữa các model trong một lượt hỏi để không tạo phiếu hỗ trợ trùng.
        Map<String, ToolExecutionResult> toolCache = new HashMap<>();
        // [0] = mã phiếu hỗ trợ vừa tạo, [1] = link form đăng yêu cầu điền sẵn
        String[] ticketHolder = new String[2];

        for (String model : orderModelsByAvailability()) {
            if (Duration.between(start, Instant.now()).compareTo(totalBudget) > 0) {
                LOG.warn("Groq time budget ({}s) exhausted before trying model '{}'", totalBudget.toSeconds(), model);
                break;
            }
            try {
                ChatOrchestrationResult result = executeWithModel(model, apiKey, systemPrompt, history, userMessage,
                        openAiTools, currentUserId, currentRole, guestContact, toolCache, ticketHolder);
                LOG.info("Received answer from Groq model '{}' in {} ms",
                        model, Duration.between(start, Instant.now()).toMillis());
                return result;
            } catch (GroqCallException ex) {
                LOG.warn("Groq model '{}' failed: {}. Trying next model...", model, ex.getMessage());
                applyCooldown(model, ex.status(), ex.retryAfterSeconds());
            } catch (Exception ex) {
                LOG.warn("Groq model '{}' failed with unexpected error: {}. Trying next model...", model, ex.toString());
                applyCooldown(model, 0, 0);
            }
        }

        LOG.error("All Groq models failed. Activating local knowledge-base synthesizer.");
        ChatOrchestrationResult fallback = fallbackSynthesizer.synthesize(userMessage, knowledgeContext);
        if (ticketHolder[0] != null) {
            return new ChatOrchestrationResult(
                    "Mình đã tạo phiếu hỗ trợ **" + ticketHolder[0] + "** cho bạn. "
                            + "Nhân viên CSKH sẽ liên hệ lại sớm nhất qua thông tin bạn đã cung cấp.",
                    List.of("Xem trạng thái phiếu " + ticketHolder[0], "Cách đăng yêu cầu sửa chữa"),
                    fallback.actions(), true, ticketHolder[0], fallback.modelUsed());
        }
        return fallback;
    }

    private List<String> orderModelsByAvailability() {
        List<String> configured = groq().getModels();
        Instant now = Instant.now();
        List<String> available = configured.stream()
                .filter(m -> {
                    Instant until = modelCooldownUntil.get(m);
                    return until == null || now.isAfter(until);
                })
                .toList();
        return available.isEmpty() ? configured : available;
    }

    private void applyCooldown(String model, int status, long retryAfterSeconds) {
        long seconds;
        if (status == 429) {
            seconds = retryAfterSeconds > 0 ? Math.min(retryAfterSeconds, 300) : groq().getCooldownSeconds();
        } else if (status == 404) {
            seconds = 3600; // model không tồn tại / đã ngừng trên Groq
        } else if (status == 401 || status == 403) {
            seconds = 0; // key sai: lỗi chung cho mọi model, không phạt riêng model nào
        } else if (status >= 500 || status == 0 || status == 504) {
            seconds = 60;
        } else {
            return;
        }
        if (seconds > 0) {
            modelCooldownUntil.put(model, Instant.now().plusSeconds(seconds));
        }
    }

    private ChatOrchestrationResult executeWithModel(
            String model,
            String apiKey,
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            List<Map<String, Object>> tools,
            Long currentUserId,
            String currentRole,
            String guestContact,
            Map<String, ToolExecutionResult> toolCache,
            String[] ticketHolder
    ) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            // Chỉ giữ vài lượt gần nhất và rút gọn câu trả lời dài để vừa hạn mức token/phút của Groq.
            int from = Math.max(0, history.size() - groq().getMaxHistoryMessages());
            for (ChatMessage msg : history.subList(from, history.size())) {
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    String text = msg.getContent();
                    int max = groq().getMaxHistoryMessageChars();
                    if (max > 0 && text.length() > max) {
                        text = text.substring(0, max) + "...";
                    }
                    messages.add(Map.of(
                            "role", "user".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant",
                            "content", text));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        int maxRounds = Math.max(1, properties.getLimits().getMaxToolRounds());
        for (int round = 0; round <= maxRounds; round++) {
            boolean forceFinalAnswer = round == maxRounds;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            if (!tools.isEmpty()) {
                payload.put("tools", tools);
                payload.put("tool_choice", forceFinalAnswer ? "none" : "auto");
            }
            payload.put("temperature", groq().getTemperature());
            payload.put("max_completion_tokens", groq().getMaxOutputTokens());
            if (model.startsWith("openai/gpt-oss") && groq().getReasoningEffort() != null
                    && !groq().getReasoningEffort().isBlank()) {
                payload.put("reasoning_effort", groq().getReasoningEffort());
            }

            JsonNode root = postWithRetry(model, apiKey, payload);
            JsonNode message = root.path("choices").path(0).path("message");
            if (message.isMissingNode()) {
                throw new GroqCallException(200, "No choice returned", 0);
            }

            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && !toolCalls.isEmpty() && !forceFinalAnswer) {
                Map<String, Object> assistantTurn = new LinkedHashMap<>();
                assistantTurn.put("role", "assistant");
                assistantTurn.put("content", message.path("content").isTextual() ? message.get("content").asText() : "");
                assistantTurn.put("tool_calls", objectMapper.convertValue(toolCalls, new TypeReference<List<Object>>() { }));
                messages.add(assistantTurn);

                for (JsonNode call : toolCalls) {
                    messages.add(executeToolCall(call, round, currentUserId, currentRole, guestContact,
                            toolCache, ticketHolder));
                }
                continue;
            }

            String content = message.path("content").asText("").trim();
            if (content.isEmpty()) {
                String finishReason = root.path("choices").path(0).path("finish_reason").asText("UNKNOWN");
                throw new GroqCallException(200, "Empty answer (finish_reason=" + finishReason + ")", 0);
            }
            try {
                ChatOrchestrationResult parsed = replyParser.parse(content, ticketHolder[0], model);
                return replyParser.withLeadingAction(parsed, PREFILL_ACTION_LABEL, ticketHolder[1]);
            } catch (IllegalStateException ex) {
                throw new GroqCallException(200, ex.getMessage(), 0);
            }
        }
        throw new GroqCallException(200, "Tool loop ended without a final answer", 0);
    }

    private Map<String, Object> executeToolCall(
            JsonNode call,
            int round,
            Long currentUserId,
            String currentRole,
            String guestContact,
            Map<String, ToolExecutionResult> toolCache,
            String[] ticketHolder
    ) {
        String toolName = call.path("function").path("name").asText();
        String rawArgs = call.path("function").path("arguments").asText("{}");
        Map<String, Object> arguments;
        try {
            arguments = rawArgs.isBlank() ? new HashMap<>()
                    : objectMapper.readValue(rawArgs, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            arguments = new HashMap<>();
        }
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        String cacheKey = toolName + "|" + rawArgs;
        ToolExecutionResult result = toolCache.get(cacheKey);
        if (result == null) {
            LOG.info("Groq round {}: calling tool '{}' with args {}", round + 1, toolName, arguments);
            result = toolPort.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);
            toolCache.put(cacheKey, result);
        } else {
            LOG.info("Groq round {}: reusing cached result of tool '{}'", round + 1, toolName);
        }

        if (("create_support_ticket".equals(toolName) || "prepare_repair_request".equals(toolName)) && !result.isError()) {
            try {
                JsonNode tr = objectMapper.readTree(result.content());
                if (tr.hasNonNull("ticketCode")) {
                    ticketHolder[0] = tr.get("ticketCode").asText();
                }
                if (tr.hasNonNull("prefillUrl")) {
                    ticketHolder[1] = tr.get("prefillUrl").asText();
                }
            } catch (Exception ignored) {
                // nội dung tool không phải JSON — bỏ qua
            }
        }

        String content = result.content() != null ? result.content() : "{}";
        int max = groq().getMaxToolResultChars();
        if (max > 0 && content.length() > max) {
            content = content.substring(0, max) + "...(đã rút gọn)";
        }
        Map<String, Object> toolMessage = new LinkedHashMap<>();
        toolMessage.put("role", "tool");
        toolMessage.put("tool_call_id", call.path("id").asText());
        toolMessage.put("content", content);
        return toolMessage;
    }

    private JsonNode postWithRetry(String model, String apiKey, Map<String, Object> payload) throws Exception {
        String url = groq().getBaseUrl() + "/chat/completions";
        int attempts = 1 + Math.max(0, groq().getMaxRetries());

        for (int attempt = 1; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(groq().getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException ex) {
                throw new GroqCallException(504, "Timeout after " + groq().getTimeoutSeconds() + "s", 0);
            }

            int status = response.statusCode();
            if (status == 200) {
                return objectMapper.readTree(response.body());
            }
            String body = response.body() != null ? response.body() : "";

            if (status == 400 && payload.containsKey("reasoning_effort") && body.contains("reasoning_effort")) {
                LOG.info("Model '{}' rejected reasoning_effort; retrying without it.", model);
                payload.remove("reasoning_effort");
                attempt--;
                continue;
            }

            double retryAfter = parseRetryAfterSeconds(response, body);
            if (status == 429 && retryAfter > 0 && retryAfter <= SHORT_WAIT_SECONDS && attempt < attempts) {
                // Hạn mức token/phút sắp hồi: chờ vài giây rồi dùng tiếp model mạnh thay vì nhảy sang model yếu hơn.
                LOG.info("Model '{}' rate-limited; waiting {}s then retrying", model, retryAfter);
                Thread.sleep((long) (retryAfter * 1000) + 250);
                continue;
            }
            // tool_use_failed: model sinh lời gọi tool sai định dạng — thử lại thường thành công.
            boolean transientError = status >= 500 || (status == 400 && body.contains("tool_use_failed"));
            if (transientError && attempt < attempts) {
                LOG.info("Model '{}' returned HTTP {} (attempt {}/{}); retrying", model, status, attempt, attempts);
                Thread.sleep(800L * attempt);
                continue;
            }
            throw new GroqCallException(status, "HTTP " + status + ": " + abbreviate(body, 300),
                    (long) Math.ceil(retryAfter));
        }
    }

    /** Thời gian chờ tối đa (giây) để thử lại cùng model khi bị giới hạn token/phút. */
    private static final double SHORT_WAIT_SECONDS = 6.0;
    private static final Pattern TRY_AGAIN_PATTERN = Pattern.compile("try again in ([0-9.]+)(ms|s)");

    /** Đọc thời gian chờ từ header retry-after, hoặc từ câu "try again in 12.2s" trong thông báo lỗi. */
    private static double parseRetryAfterSeconds(HttpResponse<String> response, String body) {
        Matcher m = TRY_AGAIN_PATTERN.matcher(body);
        if (m.find()) {
            double value = Double.parseDouble(m.group(1));
            return "ms".equals(m.group(2)) ? value / 1000.0 : value;
        }
        return response.headers().firstValue("retry-after")
                .map(v -> {
                    try {
                        return Double.parseDouble(v);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .orElse(0.0);
    }

    /** Chuyển khai báo tool kiểu Gemini (type: OBJECT/STRING...) sang định dạng function của OpenAI/Groq. */
    List<Map<String, Object>> toOpenAiTools(List<Map<String, Object>> declarations) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (declarations == null) {
            return result;
        }
        for (Map<String, Object> decl : declarations) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", decl.get("name"));
            if (decl.get("description") != null) {
                function.put("description", decl.get("description"));
            }
            Object params = decl.get("parameters");
            JsonNode schema = params != null ? objectMapper.valueToTree(params)
                    : objectMapper.createObjectNode().put("type", "object");
            lowercaseTypes(schema);
            if (schema.isObject() && !schema.has("properties")) {
                ((ObjectNode) schema).putObject("properties");
            }
            function.put("parameters", objectMapper.convertValue(schema, new TypeReference<Map<String, Object>>() { }));
            result.add(Map.of("type", "function", "function", function));
        }
        return result;
    }

    private void lowercaseTypes(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            if (obj.path("type").isTextual()) {
                obj.put("type", obj.get("type").asText().toLowerCase(Locale.ROOT));
            }
            obj.fields().forEachRemaining(e -> {
                if (!"type".equals(e.getKey())) {
                    lowercaseTypes(e.getValue());
                }
            });
        } else if (node != null && node.isArray()) {
            node.forEach(this::lowercaseTypes);
        }
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Lỗi khi gọi một model cụ thể; {@code status} là HTTP status (0 nếu không xác định). */
    static final class GroqCallException extends RuntimeException {
        private final int status;
        private final long retryAfterSeconds;

        GroqCallException(int status, String message, long retryAfterSeconds) {
            super(message);
            this.status = status;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        int status() {
            return status;
        }

        long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
