package com.fixlink.domain.exception;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String message) {
        super("USER_ALREADY_EXISTS", message);
    }
}
