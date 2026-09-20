package com.fixlink.domain.exception;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại", 401);
    }

    public InvalidRefreshTokenException(String message) {
        super("INVALID_REFRESH_TOKEN", message, 401);
    }
}
