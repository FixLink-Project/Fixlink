package com.fixlink.domain.exception;

public class PasswordSameAsOldException extends DomainException {
    public PasswordSameAsOldException() {
        super("PASSWORD_SAME_AS_OLD", "Mật khẩu mới không được trùng với mật khẩu hiện tại", 400);
    }
}
