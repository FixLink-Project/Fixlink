package com.fixlink.chatbot.application.port.out;

import com.fixlink.chatbot.domain.model.ChatMessage;
import java.util.List;
import java.util.Map;

public interface GeminiChatPort {

    /**
     * Gửi yêu cầu hội thoại tới Gemini với failover và điều phối tool calling.
     */
    ChatOrchestrationResult generateChatResponse(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            List<Map<String, Object>> tools,
            Long currentUserId,
            String currentRole,
            String currentRoute,
            String guestContact
    );

    record ChatOrchestrationResult(
            String reply,
            List<String> suggestions,
            List<Map<String, String>> actions,
            boolean needsHuman,
            String ticketId,
            String modelUsed
    ) { }
}
