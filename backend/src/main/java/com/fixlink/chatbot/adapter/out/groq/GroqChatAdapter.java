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
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties.LitellmProperties;
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
 * Gọi LLM qua endpoint tương thích OpenAI ({@code /chat/completions}) với chuỗi failover nhiều
 * model và vòng lặp tool calling.
 *
 * <p>Thứ tự ưu tiên: cổng LiteLLM (nếu cấu hình {@code chatbot.litellm.base-url}; LiteLLM tự chuyển
 * Groq → nhà cung cấp dự phòng khi hết hạn mức) → gọi thẳng Groq (khi LiteLLM không chạy) → bộ trả
 * lời cục bộ từ kho tri thức.
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

    static final String LITELLM = "litellm";
    static final String GROQ = "groq";
    static final String SERVED_MODEL_HEADER = "x-litellm-model-name";
    private static final long LITELLM_RATE_LIMIT_PAUSE_SECONDS = 10;
    private static final String SERVED_MODEL_FIELD = "_servedModel";

    /** Một đích gọi tương thích OpenAI: cổng LiteLLM hoặc Groq trực tiếp. */
    record Endpoint(String name, String baseUrl, String apiKey, List<String> models,
                    int timeoutSeconds, int maxRetries, int budgetSeconds) {

        boolean isLitellm() {
            return LITELLM.equals(name);
        }
    }

    List<Endpoint> endpoints() {
        List<Endpoint> result = new ArrayList<>();
        LitellmProperties litellm = properties.getLitellm();
        if (litellm != null && litellm.isEnabled() && !litellm.getModels().isEmpty()) {
            String base = litellm.getBaseUrl().trim().replaceAll("/+$", "");
            if (!base.contains("://")) {
                base = "http://" + base; // Render cấp địa chỉ nội bộ dạng host:port
            }
            // LiteLLM đã tự thử lại và chuyển nhà cung cấp bên trong nên không thử lại thêm ở đây.
            result.add(new Endpoint(LITELLM, base, litellm.getApiKey(), litellm.getModels(),
                    litellm.getTimeoutSeconds(), 0, litellm.getTimeoutSeconds() + 5));
        }
        String groqKey = groq().getApiKey();
        if (groqKey != null && !groqKey.isBlank()) {
            result.add(new Endpoint(GROQ, groq().getBaseUrl(), groqKey, groq().getModels(),
                    groq().getTimeoutSeconds(), groq().getMaxRetries(), groq().getTotalBudgetSeconds()));
        }
        return result;
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
        List<Endpoint> endpoints = endpoints();
        if (endpoints.isEmpty()) {
            LOG.warn("No LITELLM_BASE_URL or GROQ_API_KEY configured. Using local knowledge-base synthesizer directly.");
            return fallbackSynthesizer.synthesize(userMessage, knowledgeContext);
        }

        List<Map<String, Object>> openAiTools = toOpenAiTools(tools);
        // Dùng chung kết quả tool giữa các model trong một lượt hỏi để không tạo phiếu hỗ trợ trùng.
        Map<String, ToolExecutionResult> toolCache = new HashMap<>();
        // [0] = mã phiếu hỗ trợ vừa tạo, [1] = link form đăng yêu cầu điền sẵn
        String[] ticketHolder = new String[2];

        for (Endpoint endpoint : endpoints) {
            Instant start = Instant.now();
            Duration budget = Duration.ofSeconds(endpoint.budgetSeconds());
            for (String model : orderModelsByAvailability(endpoint)) {
                if (Duration.between(start, Instant.now()).compareTo(budget) > 0) {
                    LOG.warn("{} time budget ({}s) exhausted before trying model '{}'",
                            endpoint.name(), budget.toSeconds(), model);
                    break;
                }
                try {
                    ChatOrchestrationResult result = executeWithModel(endpoint, model, systemPrompt, history,
                            userMessage, openAiTools, currentUserId, currentRole, guestContact, toolCache, ticketHolder);
                    LOG.info("Received answer via {} (model '{}') in {} ms", endpoint.name(), result.modelUsed(),
                            Duration.between(start, Instant.now()).toMillis());
                    return result;
                } catch (GroqCallException ex) {
                    LOG.warn("{} model '{}' failed: {}. Trying next option...", endpoint.name(), model, ex.getMessage());
                    applyCooldown(endpoint, model, ex.status(), ex.retryAfterSeconds());
                } catch (Exception ex) {
                    LOG.warn("{} model '{}' failed with unexpected error: {}. Trying next option...",
                            endpoint.name(), model, ex.toString());
                    applyCooldown(endpoint, model, 0, 0);
                }
            }
        }

        LOG.error("All LLM providers failed. Activating local knowledge-base synthesizer.");
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

    private List<String> orderModelsByAvailability(Endpoint endpoint) {
        List<String> configured = endpoint.models();
        Instant now = Instant.now();
        List<String> available = configured.stream()
                .filter(m -> {
                    Instant until = modelCooldownUntil.get(endpoint.name() + "|" + m);
                    return until == null || now.isAfter(until);
                })
                .toList();
        if (available.isEmpty() && endpoint.isLitellm()) {
            return List.of(); // cổng LiteLLM đang tạm nghỉ (không kết nối được / hết hạn mức): gọi thẳng Groq
        }
        return available.isEmpty() ? configured : available;
    }

    private void applyCooldown(Endpoint endpoint, String model, int status, long retryAfterSeconds) {
        long seconds;
        if (endpoint.isLitellm() && status == 429) {
            // LiteLLM báo 429 khi mọi model Groq vừa chạm hạn mức token/phút; hạn mức này hồi lại sau vài giây
            // nên chỉ tạm bỏ qua LiteLLM rất ngắn rồi dùng lại.
            seconds = Math.min(retryAfterSeconds > 0 ? retryAfterSeconds : LITELLM_RATE_LIMIT_PAUSE_SECONDS,
                    LITELLM_RATE_LIMIT_PAUSE_SECONDS);
        } else if (endpoint.isLitellm() && status >= 500) {
            seconds = 20;
        } else if (status == 429) {
            seconds = retryAfterSeconds > 0 ? Math.min(retryAfterSeconds, 300) : groq().getCooldownSeconds();
        } else if (status == 404) {
            seconds = 3600; // model không tồn tại / đã ngừng
        } else if (status == 401 || status == 403) {
            seconds = 0; // key sai: lỗi chung cho mọi model, không phạt riêng model nào
        } else if (status >= 500 || status == 0 || status == 504) {
            seconds = 60;
        } else {
            return;
        }
        if (seconds > 0) {
            modelCooldownUntil.put(endpoint.name() + "|" + model, Instant.now().plusSeconds(seconds));
        }
    }

    private ChatOrchestrationResult executeWithModel(
            Endpoint endpoint,
            String model,
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
            // Qua LiteLLM thì reasoning_effort khai báo theo từng model trong litellm/config.yaml.
            if (!endpoint.isLitellm() && model.startsWith("openai/gpt-oss") && groq().getReasoningEffort() != null
                    && !groq().getReasoningEffort().isBlank()) {
                payload.put("reasoning_effort", groq().getReasoningEffort());
            }

            JsonNode root = postWithRetry(endpoint, model, payload);
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                // Theo dõi token để ước lượng hạn mức/ngày của gói miễn phí.
                LOG.info("{} round {} token usage: prompt={}, completion={}", endpoint.name(), round + 1,
                        usage.path("prompt_tokens").asInt(), usage.path("completion_tokens").asInt());
            }
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
                ChatOrchestrationResult parsed = replyParser.parse(content, ticketHolder[0], modelLabel(endpoint, model, root));
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

    /** Qua LiteLLM thì ghi lại model thật đã trả lời (ví dụ litellm/mistral-small-latest) để thống kê. */
    static String modelLabel(Endpoint endpoint, String requestedModel, JsonNode root) {
        if (!endpoint.isLitellm()) {
            return requestedModel;
        }
        String actual = root != null ? root.path(SERVED_MODEL_FIELD).asText(root.path("model").asText("")) : "";
        return LITELLM + "/" + (actual.isBlank() ? requestedModel : actual);
    }

    private JsonNode postWithRetry(Endpoint endpoint, String model, Map<String, Object> payload) throws Exception {
        String url = endpoint.baseUrl() + "/chat/completions";
        int attempts = 1 + Math.max(0, endpoint.maxRetries());

        for (int attempt = 1; ; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + (endpoint.apiKey() != null ? endpoint.apiKey() : ""))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(endpoint.timeoutSeconds()))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (HttpTimeoutException ex) {
                throw new GroqCallException(504, "Timeout after " + endpoint.timeoutSeconds() + "s", 0);
            }

            int status = response.statusCode();
            if (status == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                // LiteLLM trả về tên nhóm model; header này cho biết model thật đã trả lời (kể cả khi đã fallback).
                response.headers().firstValue(SERVED_MODEL_HEADER).ifPresent(served -> {
                    if (root instanceof ObjectNode obj) {
                        obj.put(SERVED_MODEL_FIELD, served);
                    }
                });
                return root;
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
