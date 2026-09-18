package com.fixlink.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per username.
 * After MAX_ATTEMPTS consecutive failures, the account is temporarily
 * locked for LOCK_DURATION_MINUTES minutes (AC-4).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final ConcurrentHashMap<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    /**
     * Check whether the given username is currently locked out.
     *
     * @return true if locked out (caller should reject with 429)
     */
    public boolean isLocked(String username) {
        AttemptInfo info = attemptsCache.get(username);
        if (info == null) {
            return false;
        }
        if (info.lockedUntil != null && LocalDateTime.now().isBefore(info.lockedUntil)) {
            return true;
        }
        // Lock has expired → clear it
        if (info.lockedUntil != null && !LocalDateTime.now().isBefore(info.lockedUntil)) {
            attemptsCache.remove(username);
            return false;
        }
        return false;
    }

    /**
     * Returns the remaining seconds until the lock expires.
     * Returns 0 if not locked.
     */
    public long getRemainingLockSeconds(String username) {
        AttemptInfo info = attemptsCache.get(username);
        if (info == null || info.lockedUntil == null) {
            return 0;
        }
        long remaining = java.time.Duration.between(LocalDateTime.now(), info.lockedUntil).getSeconds();
        return Math.max(0, remaining);
    }

    /**
     * Record a failed login attempt for the given username.
     * If the failure threshold is exceeded, the account becomes temporarily locked.
     */
    public void recordFailure(String username) {
        attemptsCache.compute(username, (key, existing) -> {
            if (existing == null) {
                return new AttemptInfo(1, null);
            }
            int newCount = existing.failedAttempts + 1;
            if (newCount >= MAX_ATTEMPTS) {
                return new AttemptInfo(newCount, LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            return new AttemptInfo(newCount, null);
        });
    }

    /**
     * Reset attempt counter on successful login.
     */
    public void recordSuccess(String username) {
        attemptsCache.remove(username);
    }

    /**
     * @return the configured maximum attempts before lock
     */
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    /**
     * @return the remaining allowed attempts for the given username
     */
    public int getRemainingAttempts(String username) {
        AttemptInfo info = attemptsCache.get(username);
        if (info == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - info.failedAttempts);
    }

    // ── Inner record ──────────────────────────────────────────

    private static class AttemptInfo {
        final int failedAttempts;
        final LocalDateTime lockedUntil;

        AttemptInfo(int failedAttempts, LocalDateTime lockedUntil) {
            this.failedAttempts = failedAttempts;
            this.lockedUntil = lockedUntil;
        }
    }
}
