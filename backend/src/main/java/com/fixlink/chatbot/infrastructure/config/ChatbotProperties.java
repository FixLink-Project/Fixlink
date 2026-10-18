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
    private GeminiProperties gemini = new GeminiProperties();
    private ToolsProperties tools = new ToolsProperties();
    private RagProperties rag = new RagProperties();
    private LimitsProperties limits = new LimitsProperties();

    @Getter
    @Setter
    public static class GeminiProperties {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private List<String> models = new ArrayList<>(List.of(
                "gemini-3.8-flash",
                "gemini-3.6-flash",
                "gemini-3.5-flash-lite",
                "gemini-3-flash-preview",
                "gemini-3.1-flash-lite"
        ));
        private int timeoutSeconds = 20;
        private int maxRetries = 1;
        private int cooldownSeconds = 60;
        private double temperature = 0.3;
        private int maxOutputTokens = 2048;
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
        private int maxToolRounds = 4;
        private int historyTurns = 10;
        private int guestRatePer10min = 20;
    }
}
