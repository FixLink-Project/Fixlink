package com.fixlink.chatbot.application.port.out;

import com.fixlink.chatbot.domain.model.ChatMessage;
import java.util.List;
import java.util.Map;

public interface LlmChatPort {

    /**
     * Gửi yêu cầu hội thoại tới Gemini với failover và điều phối tool calling.
     *
     * @param knowledgeContext đoạn tri thức RAG liên quan nhất tới câu hỏi, dùng cho bộ
     *                         trả lời dự phòng cục bộ khi toàn bộ model Gemini không khả dụng
     */
    ChatOrchestrationResult generateChatResponse(
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage,
            List<Map<String, Object>> tools,
            Long currentUserId,
            String currentRole,
            String currentRoute,
            String guestContact,
            String knowledgeContext
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
