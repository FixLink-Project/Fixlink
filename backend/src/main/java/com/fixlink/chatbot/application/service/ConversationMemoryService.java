package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.ChatSession;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationMemoryService.class);
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

    /**
     * Returns the ChatSession for the given sessionId, or null if not found.
     * Used for ownership verification in delete operations.
     */
    public ChatSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessionStore.get(sessionId);
    }

    /**
     * Scheduled task: evict expired sessions every 10 minutes.
     * Prevents unbounded memory growth from long-lived sessions exceeding the 24h TTL.
     */
    @Scheduled(fixedRate = 10 * 60 * 1000, initialDelay = 10 * 60 * 1000)
    public void evictExpiredSessions() {
        Instant now = Instant.now();
        int evicted = 0;

        Iterator<Map.Entry<String, ChatSession>> it = sessionStore.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ChatSession> entry = it.next();
            ChatSession session = entry.getValue();
            if (session.getUpdatedAt() != null && Duration.between(session.getUpdatedAt(), now).compareTo(SESSION_TTL) > 0) {
                it.remove();
                evicted++;
            }
        }

        if (evicted > 0) {
            LOG.info("Session cleanup: evicted {} expired sessions. Active sessions: {}", evicted, sessionStore.size());
        }
    }
}
