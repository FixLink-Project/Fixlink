package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import com.fixlink.domain.exception.DomainException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterService.class);
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L; // 10 minutes

    private final Map<String, Queue<Long>> requestLog = new ConcurrentHashMap<>();
    private final ChatbotProperties properties;

    public RateLimiterService(ChatbotProperties properties) {
        this.properties = properties;
    }

    public void checkRateLimit(String clientIp, Long userId) {
        String key = userId != null ? "user:" + userId : "ip:" + (clientIp != null ? clientIp : "unknown");
        int maxRequests = userId != null ? 60 : properties.getLimits().getGuestRatePer10min();

        long now = Instant.now().toEpochMilli();
        Queue<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Clean up expired timestamps outside 10 min window
        while (!timestamps.isEmpty() && now - timestamps.peek() > WINDOW_MILLIS) {
            timestamps.poll();
        }

        if (timestamps.size() >= maxRequests) {
            throw new DomainException(
                    "CHATBOT_RATE_LIMITED",
                    "Bạn đã gửi quá nhiều tin nhắn trong thời gian ngắn. Vui lòng thử lại sau ít phút.",
                    429
            );
        }

        timestamps.offer(now);
    }

    /**
     * Scheduled task: clean up stale entries in requestLog every 5 minutes.
     * Removes expired timestamps and purges empty queues to prevent memory leaks.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void cleanupStaleEntries() {
        long now = Instant.now().toEpochMilli();
        int removedKeys = 0;

        Iterator<Map.Entry<String, Queue<Long>>> it = requestLog.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Queue<Long>> entry = it.next();
            Queue<Long> timestamps = entry.getValue();

            // Evict expired timestamps
            while (!timestamps.isEmpty() && now - timestamps.peek() > WINDOW_MILLIS) {
                timestamps.poll();
            }

            // Remove entirely empty queues
            if (timestamps.isEmpty()) {
                it.remove();
                removedKeys++;
            }
        }

        if (removedKeys > 0) {
            LOG.info("RateLimiter cleanup: removed {} stale client entries. Remaining: {}", removedKeys, requestLog.size());
        }
    }
}
