package com.fixlink.application.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        if (username == null) return false;
        AttemptInfo info = attemptsCache.get(username.toLowerCase().trim());
        if (info == null) return false;

        if (info.lockedUntil != null) {
            if (Instant.now().isBefore(info.lockedUntil)) {
                return true;
            } else {
                // Hết thời gian khóa, tự động mở
                attemptsCache.remove(username.toLowerCase().trim());
                return false;
            }
        }
        return false;
    }

    public long getRemainingLockSeconds(String username) {
        if (username == null) return 0;
        AttemptInfo info = attemptsCache.get(username.toLowerCase().trim());
        if (info == null || info.lockedUntil == null) return 0;

        long diff = Duration.between(Instant.now(), info.lockedUntil).getSeconds();
        return Math.max(0, diff);
    }

    public int recordFailure(String username) {
        if (username == null) return 1;
        String key = username.toLowerCase().trim();

        AttemptInfo info = attemptsCache.compute(key, (k, current) -> {
            if (current == null) {
                return new AttemptInfo(1, Instant.now(), null);
            }
            // Nếu lần thử trước đã quá 15 phút, tính lại từ đầu
            if (Duration.between(current.lastAttemptAt, Instant.now()).compareTo(LOCK_DURATION) > 0) {
                return new AttemptInfo(1, Instant.now(), null);
            }

            int newCount = current.failedCount + 1;
            Instant lockTime = (newCount >= MAX_FAILED_ATTEMPTS) ? Instant.now().plus(LOCK_DURATION) : null;
            return new AttemptInfo(newCount, Instant.now(), lockTime);
        });

        return info.failedCount;
    }

    public void recordSuccess(String username) {
        if (username != null) {
            attemptsCache.remove(username.toLowerCase().trim());
        }
    }

    public int getFailedAttempts(String username) {
        if (username == null) return 0;
        AttemptInfo info = attemptsCache.get(username.toLowerCase().trim());
        return info != null ? info.failedCount : 0;
    }

    public void reset(String username) {
        if (username != null) {
            attemptsCache.remove(username.toLowerCase().trim());
        }
    }

    private static class AttemptInfo {
        int failedCount;
        Instant lastAttemptAt;
        Instant lockedUntil;

        AttemptInfo(int failedCount, Instant lastAttemptAt, Instant lockedUntil) {
            this.failedCount = failedCount;
            this.lastAttemptAt = lastAttemptAt;
            this.lockedUntil = lockedUntil;
        }
    }
}
