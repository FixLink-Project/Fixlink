package com.fixlink.domain.exception;

public class AccountTemporarilyLockedException extends DomainException {

    private final long remainingSeconds;

    public AccountTemporarilyLockedException(String message) {
        super("ACCOUNT_TEMPORARILY_LOCKED", message);
        this.remainingSeconds = 900;
    }

    public AccountTemporarilyLockedException(String message, long remainingSeconds) {
        super("ACCOUNT_TEMPORARILY_LOCKED", message);
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
