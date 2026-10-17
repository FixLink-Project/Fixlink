package com.fixlink.chatbot.adapter.out.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.GeminiChatPort;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResilientGeminiChatAdapter implements GeminiChatPort {

    private static final Logger LOG = LoggerFactory.getLogger(ResilientGeminiChatAdapter.class);

    private static final Set<String> ALLOWED_ROUTE_PREFIXES = Set.of(
            "/",
            "/dang-yeu-cau",
            "/yeu-cau-cua-toi",
            "/yeu-cau",
            "/dang-nhap",
            "/dang-ky",
            "/dang-ky-tho",
            "/quen-mat-khau",
            "/dat-lai-mat-khau",
            "/tho",
            "/tho/ho-so",
            "/tho/yeu-cau"
    );

    private final ChatbotProperties properties;
    private final ChatbotToolPort toolPort;
    private final ExtractiveFallbackSynthesizer fallbackSynthesizer;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, Instant> modelCooldownUntil = new ConcurrentHashMap<>();

    public ResilientGeminiChatAdapter(
            ChatbotProperties properties,
            ChatbotToolPort toolPort,
            ExtractiveFallbackSynthesizer fallbackSynthesizer,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.toolPort = toolPort;
        this.fallbackSynthesizer = fallbackSynthesizer;
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
            String guestContact
    ) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("No GEMINI_API_KEY configured. Using local extractive synthesizer directly.");
            return fallbackSynthesizer.synthesize(userMessage, systemPrompt);
        }

        List<String> models = properties.getGemini().getModels();
        Instant now = Instant.now();
        Exception lastException = null;

        for (String modelName : models) {
            Instant cooldown = modelCooldownUntil.get(modelName);
            if (cooldown != null && now.isBefore(cooldown)) {
                LOG.debug("Model '{}' is in cooldown until {}. Skipping...", modelName, cooldown);
                continue;
            }

            try {
                ChatOrchestrationResult result = executeChatWithModel(
                        modelName,
                        apiKey,
                        systemPrompt,
                        history,
                        userMessage,
                        tools,
                        currentUserId,
                        currentRole,
                        guestContact
                );
                if (result != null && result.reply() != null && !result.reply().isBlank()) {
                    LOG.info("Successfully received answer from Gemini model '{}'", modelName);
                    return result;
                }
            } catch (Exception ex) {
                lastException = ex;
                String msg = ex.getMessage() != null ? ex.getMessage() : "";
                LOG.warn("Gemini model '{}' failed with error: {}. Attempting fallback...", modelName, msg);

                if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("quota")) {
                    modelCooldownUntil.put(modelName, now.plusSeconds(300));
                } else if (msg.contains("503") || msg.contains("UNAVAILABLE") || msg.contains("timeout")) {
                    modelCooldownUntil.put(modelName, now.plusSeconds(60));
                } else {
                    modelCooldownUntil.put(modelName, now.plusSeconds(30));
                }
            }
        }

        LOG.error("All Gemini models failed. Activating extractive local synthesizer.", lastException);
        return fallbackSynthesizer.synthesize(userMessage, systemPrompt);
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
            String guestContact
    ) throws Exception {
        String url = properties.getGemini().getBaseUrl() + "/models/" + modelName + ":generateContent";

        // Build contents list
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add history (up to last 10 turns)
        if (history != null) {
            for (ChatMessage msg : history) {
                String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        // Add current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        // Format tools
        List<Map<String, Object>> geminiTools = List.of(Map.of("functionDeclarations", tools));

        int maxRounds = properties.getLimits().getMaxToolRounds();
        String createdTicketId = null;

        for (int round = 0; round < maxRounds; round++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            payload.put("contents", contents);
            payload.put("tools", geminiTools);
            payload.put("toolConfig", Map.of("functionCallingConfig", Map.of("mode", "AUTO")));
            payload.put("generationConfig", Map.of(
                    "temperature", properties.getGemini().getTemperature(),
                    "maxOutputTokens", properties.getGemini().getMaxOutputTokens()
            ));

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode candidate = rootNode.path("candidates").path(0);
            JsonNode parts = candidate.path("content").path("parts");

            JsonNode functionCallNode = null;
            StringBuilder textContent = new StringBuilder();

            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        functionCallNode = part.get("functionCall");
                    }
                    if (part.has("text")) {
                        textContent.append(part.get("text").asText()).append(" ");
                    }
                }
            }

            // Check if model wants to call a function
            if (functionCallNode != null) {
                String toolName = functionCallNode.path("name").asText();
                JsonNode argsNode = functionCallNode.path("args");
                Map<String, Object> arguments = objectMapper.convertValue(argsNode, Map.class);
                LOG.info("Gemini round {}: Calling tool '{}' with args {}", round + 1, toolName, arguments);

                // Execute tool
                var toolResult = toolPort.executeTool(toolName, arguments, currentUserId, currentRole, guestContact);

                // Track ticket creation
                if ("create_support_ticket".equals(toolName) && !toolResult.isError()) {
                    try {
                        JsonNode tr = objectMapper.readTree(toolResult.content());
                        if (tr.has("ticketCode")) {
                            createdTicketId = tr.get("ticketCode").asText();
                        }
                    } catch (Exception ignored) { }
                }

                // Append functionCall to history
                contents.add(Map.of(
                        "role", "model",
                        "parts", List.of(Map.of(
                                "functionCall", Map.of(
                                        "name", toolName,
                                        "args", arguments
                                )
                        ))
                ));

                // Append functionResponse to history
                Map<String, Object> responseObj = Map.of(
                        "name", toolName,
                        "response", Map.of("content", toolResult.content())
                );
                contents.add(Map.of(
                        "role", "function",
                        "parts", List.of(Map.of("functionResponse", responseObj))
                ));

                // Continue to next round
                continue;
            }

            // No function calls, model returned final text
            String rawText = textContent.toString().trim();
            return parseFinalOutput(rawText, createdTicketId, modelName);
        }

        // Exhausted tool rounds, synthesize fallback
        return fallbackSynthesizer.synthesize(userMessage, systemPrompt);
    }

    private ChatOrchestrationResult parseFinalOutput(String rawText, String ticketId, String modelName) {
        String cleanJson = rawText;

        // Strip code fences if present (```json ... ```)
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        // Check for JSON object boundaries
        int startBrace = cleanJson.indexOf('{');
        int endBrace = cleanJson.lastIndexOf('}');
        if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
            cleanJson = cleanJson.substring(startBrace, endBrace + 1);
        }

        try {
            JsonNode root = objectMapper.readTree(cleanJson);
            String reply = root.has("reply") ? root.get("reply").asText() : rawText;

            List<String> suggestions = new ArrayList<>();
            if (root.has("suggestions") && root.get("suggestions").isArray()) {
                for (JsonNode s : root.get("suggestions")) {
                    suggestions.add(s.asText());
                }
            }

            List<Map<String, String>> actions = new ArrayList<>();
            if (root.has("actions") && root.get("actions").isArray()) {
                for (JsonNode a : root.get("actions")) {
                    String type = a.path("type").asText("link");
                    String label = a.path("label").asText("Xem thêm");
                    String url = a.path("url").asText("");

                    // Output Guard: Validate whitelist route
                    if (isValidInternalUrl(url)) {
                        actions.add(Map.of("type", type, "label", label, "url", url));
                    }
                }
            }

            boolean needsHuman = root.has("needsHuman") && root.get("needsHuman").asBoolean();

            return new ChatOrchestrationResult(
                    reply,
                    suggestions,
                    actions,
                    needsHuman,
                    ticketId,
                    modelName
            );
        } catch (Exception ex) {
            LOG.warn("Failed to parse structured JSON from LLM: {}. Falling back to cleaned text.", ex.getMessage());
            return new ChatOrchestrationResult(
                    rawText,
                    List.of("Đăng yêu cầu", "Tra cứu dịch vụ", "Gặp nhân viên hỗ trợ"),
                    List.of(Map.of("type", "link", "label", "Trang chủ", "url", "/")),
                    false,
                    ticketId,
                    modelName
            );
        }
    }

    private boolean isValidInternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String clean = url.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("//")) {
            return false; // Disallow external links
        }
        for (String prefix : ALLOWED_ROUTE_PREFIXES) {
            if (clean.equals(prefix) || clean.startsWith(prefix + "/") || clean.startsWith(prefix + "?")) {
                return true;
            }
        }
        return false;
    }

    public String sanitizeOutputLinks(String text) {
        if (text == null) {
            return "";
        }
        // Scrub external HTTP/HTTPS links from markdown syntax [label](http://...) -> label
        return text.replaceAll("\\[([^\\]]+)\\]\\((https?:\\/\\/[^\\)]+)\\)", "$1");
    }
}
