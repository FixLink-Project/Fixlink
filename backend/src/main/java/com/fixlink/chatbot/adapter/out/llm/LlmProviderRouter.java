package com.fixlink.chatbot.adapter.out.llm;

import com.fixlink.chatbot.adapter.out.gemini.ResilientGeminiChatAdapter;
import com.fixlink.chatbot.adapter.out.groq.GroqChatAdapter;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Chọn nhà cung cấp LLM theo cấu hình {@code chatbot.provider} (groq | gemini). */
@Component
@Primary
public class LlmProviderRouter implements LlmChatPort {

    private final GroqChatAdapter groqAdapter;
    private final ResilientGeminiChatAdapter geminiAdapter;
    private final ChatbotProperties properties;

    public LlmProviderRouter(
            GroqChatAdapter groqAdapter,
            ResilientGeminiChatAdapter geminiAdapter,
            ChatbotProperties properties
    ) {
        this.groqAdapter = groqAdapter;
        this.geminiAdapter = geminiAdapter;
        this.properties = properties;
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
        LlmChatPort delegate = properties.isGroqProvider() ? groqAdapter : geminiAdapter;
        return delegate.generateChatResponse(systemPrompt, history, userMessage, tools, currentUserId,
                currentRole, currentRoute, guestContact, knowledgeContext);
    }
}
