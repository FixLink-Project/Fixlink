package com.fixlink.exception;

/**
 * Thrown when a user exceeds the maximum allowed login attempts
 * and their account is temporarily locked (AC-4).
 */
public class TooManyRequestsException extends RuntimeException {

    private final String errorCode;
    private final long remainingSeconds;

    public TooManyRequestsException(String message, long remainingSeconds) {
        super(message);
        this.errorCode = "ACCOUNT_TEMPORARILY_LOCKED";
        this.remainingSeconds = remainingSeconds;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
