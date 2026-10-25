package com.fixlink.chatbot.adapter.out.groq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.chatbot.adapter.out.gemini.ExtractiveFallbackSynthesizer;
import com.fixlink.chatbot.adapter.out.llm.ChatReplyParser;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử hợp đồng với Groq (API tương thích OpenAI) bằng máy chủ HTTP giả lập cục bộ. */
class GroqChatAdapterContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<JsonNode> receivedRequests = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private ChatbotToolPort toolPort;
    private GroqChatAdapter adapter;
    private Function<JsonNode, FakeResponse> responder;

    private record FakeResponse(int status, String body) { }

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            JsonNode body = objectMapper.readTree(exchange.getRequestBody().readAllBytes());
            receivedRequests.add(body);
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            FakeResponse response = responder.apply(body);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        ChatbotProperties properties = new ChatbotProperties();
        properties.getGroq().setApiKey("gsk_test");
        properties.getGroq().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1");
        properties.getGroq().setModels(List.of("openai/gpt-oss-120b", "llama-3.3-70b-versatile"));
        properties.getGroq().setTimeoutSeconds(5);

        toolPort = mock(ChatbotToolPort.class);
        adapter = new GroqChatAdapter(properties, toolPort, new ExtractiveFallbackSynthesizer(),
                new ChatReplyParser(objectMapper), objectMapper);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static final List<Map<String, Object>> TOOLS = List.of(Map.of(
            "name", "calculate_deposit",
            "description", "Tính tiền cọc",
            "parameters", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of("amount", Map.of("type", "NUMBER", "description", "VND")),
                    "required", List.of("amount"))));

    private LlmChatPort.ChatOrchestrationResult ask(String question) {
        return adapter.generateChatResponse("system", List.of(), question, TOOLS, null, "GUEST", "/", null, "");
    }

    @Test
    @DisplayName("Vòng tool calling chuẩn OpenAI: gửi tool_calls và kết quả tool kèm tool_call_id")
    void shouldRunOpenAiToolLoop() {
        when(toolPort.executeTool(eq("calculate_deposit"), anyMap(), any(), any(), any()))
                .thenReturn(new ChatbotToolPort.ToolExecutionResult("{\"depositText\":\"240.000 đ\"}", false));

        responder = body -> {
            JsonNode messages = body.path("messages");
            JsonNode last = messages.get(messages.size() - 1);
            if ("tool".equals(last.path("role").asText())) {
                return new FakeResponse(200, """
                        {"choices":[{"message":{"role":"assistant","content":"{\\"reply\\":\\"Tiền cọc là **240.000 đ**.\\",\\"suggestions\\":[\\"Khi nào trả phần còn lại?\\"],\\"actions\\":[],\\"needsHuman\\":false}"},"finish_reason":"stop"}]}
                        """);
            }
            return new FakeResponse(200, """
                    {"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[
                      {"id":"call_9","type":"function","function":{"name":"calculate_deposit","arguments":"{\\"amount\\":800000}"}}
                    ]},"finish_reason":"tool_calls"}]}
                    """);
        };

        LlmChatPort.ChatOrchestrationResult result = ask("Ngân sách 800 nghìn thì cọc bao nhiêu?");

        assertThat(result.modelUsed()).isEqualTo("openai/gpt-oss-120b");
        assertThat(result.reply()).contains("240.000 đ");
        assertThat(result.suggestions()).containsExactly("Khi nào trả phần còn lại?");
        verify(toolPort).executeTool(eq("calculate_deposit"), eq(Map.of("amount", 800000)), any(), any(), any());

        JsonNode first = receivedRequests.get(0);
        assertThat(authHeaders.get(0)).isEqualTo("Bearer gsk_test");
        assertThat(first.path("reasoning_effort").asText()).isEqualTo("low");
        JsonNode params = first.path("tools").path(0).path("function").path("parameters");
        assertThat(params.path("type").asText()).isEqualTo("object");
        assertThat(params.path("properties").path("amount").path("type").asText()).isEqualTo("number");

        JsonNode second = receivedRequests.get(1).path("messages");
        JsonNode toolMessage = second.get(second.size() - 1);
        assertThat(toolMessage.path("tool_call_id").asText()).isEqualTo("call_9");
        assertThat(second.get(second.size() - 2).path("tool_calls").path(0).path("id").asText()).isEqualTo("call_9");
    }

    @Test
    @DisplayName("Model chính chạm hạn mức (429) thì chuyển sang model dự phòng")
    void shouldFailOverOnRateLimit() {
        responder = body -> body.path("model").asText().equals("openai/gpt-oss-120b")
                ? new FakeResponse(429, "{\"error\":{\"message\":\"Rate limit reached\"}}")
                : new FakeResponse(200, """
                        {"choices":[{"message":{"role":"assistant","content":"Chào bạn, mình có thể giúp gì?"},"finish_reason":"stop"}]}
                        """);

        LlmChatPort.ChatOrchestrationResult result = ask("xin chào");

        assertThat(result.modelUsed()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(result.reply()).isEqualTo("Chào bạn, mình có thể giúp gì?");
        assertThat(receivedRequests.get(1).has("reasoning_effort")).isFalse();
    }

    @Test
    @DisplayName("Key sai (401) thì trả lời bằng bộ FAQ cục bộ, không văng lỗi")
    void shouldFallbackLocallyOnInvalidKey() {
        responder = body -> new FakeResponse(401, "{\"error\":{\"message\":\"Invalid API Key\"}}");

        LlmChatPort.ChatOrchestrationResult result = ask("Tiền cọc tính thế nào?");

        assertThat(result.modelUsed()).startsWith("local-");
        assertThat(result.reply()).contains("30%");
    }
}
