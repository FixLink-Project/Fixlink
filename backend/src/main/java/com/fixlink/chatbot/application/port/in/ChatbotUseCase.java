package com.fixlink.chatbot.application.port.in;

import com.fixlink.chatbot.domain.model.ChatAction;
import com.fixlink.chatbot.domain.model.SourceReference;
import java.util.List;

public interface ChatbotUseCase {

    ChatResponse handleMessage(ChatCommand command);

    List<String> getSuggestions(String route, String role);

    void recordFeedback(String sessionId, String messageId, Long userId, String rating, String comment);

    void deleteSession(String sessionId);

    record ChatCommand(
            String sessionId,
            String message,
            String route,
            Long userId,
            String role,
            String clientIp,
            String guestContact
    ) { }

    record ChatResponse(
            String reply,
            List<String> suggestions,
            List<ChatAction> actions,
            List<SourceReference> sources,
            boolean needsHuman,
            String ticketId,
            String sessionId,
            String modelUsed
    ) { }
}
