package com.fixlink.domain.exception;

public class AccountInactiveException extends DomainException {
    public AccountInactiveException(String message) {
        super("ACCOUNT_INACTIVE", message);
    }
}
