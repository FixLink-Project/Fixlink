package com.fixlink.chatbot.adapter.out.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Kiểm thử hợp đồng với Gemini API bằng một máy chủ HTTP giả lập cục bộ.
 *
 * <p>Bảo vệ lỗi gốc từng làm chatbot luôn trả "văn bản mẫu": Gemini 3 bắt buộc lượt model chứa
 * functionCall phải được gửi lại kèm thoughtSignature; thiếu chữ ký thì API trả HTTP 400.
 */
class GeminiToolLoopContractTest {

    private static final String SIGNATURE = "SIG-abc123";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<JsonNode> receivedRequests = new CopyOnWriteArrayList<>();
    private final List<String> requestedModels = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private ChatbotProperties properties;
    private ChatbotToolPort toolPort;
    private ResilientGeminiChatAdapter adapter;
    private Function<JsonNode, FakeResponse> responder;

    private record FakeResponse(int status, String body) { }

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            JsonNode body = objectMapper.readTree(exchange.getRequestBody().readAllBytes());
            receivedRequests.add(body);
            String path = exchange.getRequestURI().getPath();
            requestedModels.add(path.substring(path.indexOf("/models/") + 8, path.indexOf(':')));
            FakeResponse response = responder.apply(body);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        properties = new ChatbotProperties();
        properties.getGemini().setApiKey("test-key");
        properties.getGemini().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta");
        properties.getGemini().setModels(List.of("gemini-3.8-flash", "gemini-3.6-flash"));
        properties.getGemini().setTimeoutSeconds(5);

        toolPort = mock(ChatbotToolPort.class);
        adapter = new ResilientGeminiChatAdapter(properties, toolPort, new ExtractiveFallbackSynthesizer(),
                new ChatReplyParser(objectMapper), objectMapper);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private LlmChatPort.ChatOrchestrationResult ask(String question) {
        return adapter.generateChatResponse("system", List.of(), question, List.of(Map.of("name", "list_service_areas")),
                7L, "CUSTOMER", "/", null, "");
    }

    @Test
    @DisplayName("Gửi lại nguyên vẹn thoughtSignature và id của functionCall ở vòng tool kế tiếp")
    void shouldEchoThoughtSignatureAndFunctionCallId() {
        when(toolPort.executeTool(eq("list_service_areas"), anyMap(), any(), any(), any()))
                .thenReturn(new ChatbotToolPort.ToolExecutionResult("{\"areas\":[\"Quận 1\",\"Quận 3\"]}", false));

        responder = body -> {
            JsonNode contents = body.path("contents");
            JsonNode last = contents.get(contents.size() - 1);
            if (last.path("parts").path(0).has("functionResponse")) {
                JsonNode modelTurn = contents.get(contents.size() - 2);
                if (!SIGNATURE.equals(modelTurn.path("parts").path(0).path("thoughtSignature").asText())) {
                    return new FakeResponse(400, "{\"error\":{\"message\":\"Function call is missing a thought_signature\"}}");
                }
                return new FakeResponse(200, """
                        {"candidates":[{"content":{"role":"model","parts":[
                          {"text":"internal reasoning","thought":true},
                          {"text":"{\\"reply\\":\\"FixLink phục vụ **Quận 1** và **Quận 3**.\\",\\"suggestions\\":[\\"Cách đăng yêu cầu\\"],\\"actions\\":[{\\"type\\":\\"link\\",\\"label\\":\\"Đăng yêu cầu\\",\\"url\\":\\"/dang-yeu-cau\\"},{\\"type\\":\\"link\\",\\"label\\":\\"Lừa đảo\\",\\"url\\":\\"https://evil.example\\"}],\\"needsHuman\\":false}"}
                        ]},"finishReason":"STOP"}]}
                        """);
            }
            return new FakeResponse(200, """
                    {"candidates":[{"content":{"role":"model","parts":[
                      {"functionCall":{"name":"list_service_areas","args":{},"id":"call_1"},"thoughtSignature":"%s"}
                    ]},"finishReason":"STOP"}]}
                    """.formatted(SIGNATURE));
        };

        LlmChatPort.ChatOrchestrationResult result = ask("FixLink phục vụ quận nào?");

        assertThat(result.modelUsed()).isEqualTo("gemini-3.8-flash");
        assertThat(result.reply()).contains("Quận 1").doesNotContain("internal reasoning");
        assertThat(result.suggestions()).containsExactly("Cách đăng yêu cầu");
        assertThat(result.actions()).extracting(a -> a.get("url")).containsExactly("/dang-yeu-cau");

        JsonNode secondRequest = receivedRequests.get(1);
        JsonNode functionResponse = secondRequest.path("contents").get(2).path("parts").path(0).path("functionResponse");
        assertThat(functionResponse.path("id").asText()).isEqualTo("call_1");
        assertThat(functionResponse.path("response").path("areas").isArray()).isTrue();
        assertThat(secondRequest.path("generationConfig").path("maxOutputTokens").asInt()).isGreaterThanOrEqualTo(4096);
    }

    @Test
    @DisplayName("Model chính quá tải (503) thì tự chuyển sang model dự phòng, không rơi về văn bản mẫu")
    void shouldFailOverToNextModelOn503() {
        responder = body -> requestedModels.get(requestedModels.size() - 1).equals("gemini-3.8-flash")
                ? new FakeResponse(503, "{\"error\":{\"status\":\"UNAVAILABLE\"}}")
                : new FakeResponse(200, """
                        {"candidates":[{"content":{"parts":[{"text":"Chào bạn, mình có thể giúp gì?"}]},"finishReason":"STOP"}]}
                        """);

        LlmChatPort.ChatOrchestrationResult result = ask("xin chào");

        assertThat(result.modelUsed()).isEqualTo("gemini-3.6-flash");
        assertThat(result.reply()).isEqualTo("Chào bạn, mình có thể giúp gì?");
        // 1 lần gọi + 1 lần retry cho model chính, rồi 1 lần cho model dự phòng
        assertThat(requestedModels).containsExactly("gemini-3.8-flash", "gemini-3.8-flash", "gemini-3.6-flash");
    }

    @Test
    @DisplayName("Không tạo phiếu hỗ trợ trùng khi model đầu lỗi sau khi đã gọi tool tạo phiếu")
    void shouldNotCreateDuplicateTicketOnFailover() {
        when(toolPort.executeTool(eq("create_support_ticket"), anyMap(), any(), any(), any()))
                .thenReturn(new ChatbotToolPort.ToolExecutionResult("{\"success\":true,\"ticketCode\":\"TCK-0000ABCD\"}", false));

        responder = body -> {
            JsonNode contents = body.path("contents");
            boolean afterTool = contents.get(contents.size() - 1).path("parts").path(0).has("functionResponse");
            String model = requestedModels.get(requestedModels.size() - 1);
            if (afterTool && model.equals("gemini-3.8-flash")) {
                return new FakeResponse(500, "{}");
            }
            if (afterTool) {
                return new FakeResponse(200, """
                        {"candidates":[{"content":{"parts":[{"text":"{\\"reply\\":\\"Đã tạo phiếu TCK-0000ABCD\\",\\"needsHuman\\":true}"}]}}]}
                        """);
            }
            return new FakeResponse(200, """
                    {"candidates":[{"content":{"parts":[
                      {"functionCall":{"name":"create_support_ticket","args":{"title":"Khiếu nại","description":"Thợ không đến"}},"thoughtSignature":"S"}
                    ]}}]}
                    """);
        };

        LlmChatPort.ChatOrchestrationResult result = ask("Tạo phiếu giúp tôi");

        assertThat(result.ticketId()).isEqualTo("TCK-0000ABCD");
        assertThat(result.needsHuman()).isTrue();
        verify(toolPort, times(1)).executeTool(eq("create_support_ticket"), anyMap(), any(), any(), any());
    }

    @Test
    @DisplayName("Tất cả model lỗi thì trả lời đúng trọng tâm từ bộ FAQ cục bộ thay vì in system prompt")
    void shouldAnswerFromLocalFaqWhenAllModelsFail() {
        responder = body -> new FakeResponse(429, "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"PerDay quota\"}}");

        LlmChatPort.ChatOrchestrationResult result = ask("Tiền cọc tính thế nào?");

        assertThat(result.modelUsed()).isEqualTo("local-faq");
        assertThat(result.reply()).contains("30%").contains("70%").doesNotContain("system");
    }
}
