package com.fixlink.chatbot.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chatbot")
@Getter
@Setter
public class ChatbotProperties {

    private boolean enabled = true;
    /** Nhà cung cấp LLM: groq | gemini. */
    private String provider = "groq";
    private GroqProperties groq = new GroqProperties();
    private LitellmProperties litellm = new LitellmProperties();
    private GeminiProperties gemini = new GeminiProperties();
    private ToolsProperties tools = new ToolsProperties();
    private RagProperties rag = new RagProperties();
    private LimitsProperties limits = new LimitsProperties();

    public boolean isGroqProvider() {
        return !"gemini".equalsIgnoreCase(provider);
    }

    /**
     * Gemini có ngữ cảnh rất lớn nên nạp nguyên kho tri thức vào prompt; Groq (gói miễn phí giới hạn
     * vài nghìn token/phút) chỉ nạp các đoạn RAG liên quan.
     */
    public boolean usesFullKnowledgeInPrompt() {
        return isGroqProvider() ? groq.isFullKnowledge() : true;
    }

    @Getter
    @Setter
    public static class GroqProperties {
        private String apiKey = "";
        /** Key Groq của tài khoản thứ hai (tùy chọn): dùng khi key chính hết hạn mức. */
        private String apiKey2 = "";
        /** Endpoint tương thích OpenAI của Groq. */
        private String baseUrl = "https://api.groq.com/openai/v1";
        /** Chuỗi failover: model đầu tiên là model chính. */
        private List<String> models = new ArrayList<>(List.of(
                "openai/gpt-oss-120b",
                "qwen/qwen3.8-27b"
        ));
        private int timeoutSeconds = 30;
        private int totalBudgetSeconds = 60;
        private int maxRetries = 1;
        private int cooldownSeconds = 60;
        private double temperature = 0.4;
        private int maxOutputTokens = 800;
        /** Mức suy luận cho model gpt-oss (low | medium | high); model khác bỏ qua. */
        private String reasoningEffort = "low";
        /** true = nạp nguyên kho tri thức vào prompt (cần gói Groq có hạn mức token/phút cao). */
        private boolean fullKnowledge = false;
        /** Số ký tự tối đa của đoạn tri thức RAG đưa vào prompt. */
        private int maxKnowledgeChars = 1800;
        /** Số ký tự tối đa của kết quả một lần gọi tool gửi lại cho model. */
        private int maxToolResultChars = 2000;
        /** Số tin nhắn lịch sử gần nhất gửi kèm mỗi request. */
        private int maxHistoryMessages = 6;
        /** Rút gọn mỗi tin nhắn lịch sử dài hơn số ký tự này. */
        private int maxHistoryMessageChars = 600;
    }

    /**
     * Cổng LiteLLM (proxy tương thích OpenAI) đứng trước các nhà cung cấp LLM. LiteLLM tự chuyển
     * sang nhà cung cấp dự phòng khi Groq hết hạn mức. Bỏ trống {@code baseUrl} thì backend gọi thẳng
     * Groq như cũ.
     */
    @Getter
    @Setter
    public static class LitellmProperties {
        /** Ví dụ http://localhost:4000 (không có /v1 ở cuối cũng được). */
        private String baseUrl = "";
        /** Master key hoặc virtual key của LiteLLM. */
        private String apiKey = "";
        /** Tên nhóm model khai báo trong litellm/config.yaml; chuỗi fallback do LiteLLM xử lý. */
        private List<String> models = new ArrayList<>(List.of("fixlink-chat"));
        /** LiteLLM có thể thử lần lượt nhiều nhà cung cấp trong một request nên cần thời gian chờ dài hơn. */
        private int timeoutSeconds = 75;

        public boolean isEnabled() {
            return baseUrl != null && !baseUrl.isBlank();
        }
    }

    @Getter
    @Setter
    public static class GeminiProperties {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        /** Chuỗi failover: gemini-3.6-flash là model chính, các model nhẹ/ổn định làm dự phòng. */
        private List<String> models = new ArrayList<>(List.of(
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite"
        ));
        /** Timeout cho mỗi HTTP request tới Gemini. */
        private int timeoutSeconds = 25;
        /** Tổng thời gian tối đa cho một câu hỏi (mọi model + mọi vòng tool) trước khi dùng fallback. */
        private int totalBudgetSeconds = 90;
        /** Số lần thử lại cùng model khi gặp lỗi tạm thời (503/500/429 theo phút). */
        private int maxRetries = 1;
        /** Thời gian tạm ngưng model sau khi chạm quota (HTTP 429). */
        private int cooldownSeconds = 60;
        /** Gemini 3 khuyến nghị giữ temperature = 1.0; hạ thấp có thể gây lặp/giảm chất lượng suy luận. */
        private double temperature = 1.0;
        /** Gồm cả token suy luận (thinking) của Gemini 3 nên cần đủ rộng để JSON không bị cắt. */
        private int maxOutputTokens = 8192;
        /** Mức suy luận của Gemini 3 (minimal | low | medium | high). Để trống để dùng mặc định của model. */
        private String thinkingLevel = "low";
    }

    @Getter
    @Setter
    public static class ToolsProperties {
        private String mode = "native"; // native | mcp
        private McpProperties mcp = new McpProperties();
    }

    @Getter
    @Setter
    public static class McpProperties {
        private String command = "node";
        private List<String> args = new ArrayList<>(List.of("../MCPSERVER/server.js"));
        private String serverPath = "../MCPSERVER/server.js";
    }

    @Getter
    @Setter
    public static class RagProperties {
        private int topK = 6;
        private double minScore = 0.35;
        private int window = 1;
    }

    @Getter
    @Setter
    public static class LimitsProperties {
        private int maxInputChars = 1000;
        private int maxToolRounds = 5;
        private int historyTurns = 10;
        private int guestRatePer10min = 40;
    }
}
