package com.fixlink.domain.exception;

public class AccountBlockedException extends DomainException {
    public AccountBlockedException(String message) {
        super("ACCOUNT_BLOCKED", message);
    }
}
