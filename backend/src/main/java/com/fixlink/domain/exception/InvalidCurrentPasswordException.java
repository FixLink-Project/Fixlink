package com.fixlink.domain.exception;

public class InvalidCurrentPasswordException extends DomainException {
    public InvalidCurrentPasswordException() {
        super("INVALID_CURRENT_PASSWORD", "Mật khẩu hiện tại không chính xác", 400);
    }
}
