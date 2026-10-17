package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.ChatSession;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final Map<String, ChatSession> sessionStore = new ConcurrentHashMap<>();
    private final ChatbotProperties properties;

    public ConversationMemoryService(ChatbotProperties properties) {
        this.properties = properties;
    }

    public ChatSession getOrCreateSession(String sessionId, Long userId, String role) {
        Instant now = Instant.now();

        if (sessionId != null && !sessionId.isBlank()) {
            ChatSession existing = sessionStore.get(sessionId);
            if (existing != null) {
                // Check TTL
                if (existing.getUpdatedAt() != null && Duration.between(existing.getUpdatedAt(), now).compareTo(SESSION_TTL) > 0) {
                    sessionStore.remove(sessionId);
                } else {
                    // Update user/role if logged in
                    if (userId != null) {
                        existing.setUserId(userId);
                        existing.setRole(role);
                    }
                    existing.setUpdatedAt(now);
                    return existing;
                }
            }
        }

        String newId = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        ChatSession newSession = ChatSession.builder()
                .sessionId(newId)
                .userId(userId)
                .role(role != null ? role : "GUEST")
                .messages(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        sessionStore.put(newId, newSession);
        return newSession;
    }

    public void addMessage(String sessionId, ChatMessage message) {
        ChatSession session = sessionStore.get(sessionId);
        if (session != null) {
            session.getMessages().add(message);
            session.setUpdatedAt(Instant.now());

            // Limit history turns
            int maxTurns = properties.getLimits().getHistoryTurns();
            if (session.getMessages().size() > maxTurns * 2) {
                int excess = session.getMessages().size() - (maxTurns * 2);
                session.setMessages(new ArrayList<>(session.getMessages().subList(excess, session.getMessages().size())));
            }
        }
    }

    public List<ChatMessage> getHistory(String sessionId) {
        ChatSession session = sessionStore.get(sessionId);
        if (session != null) {
            return new ArrayList<>(session.getMessages());
        }
        return List.of();
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessionStore.remove(sessionId);
        }
    }
}
