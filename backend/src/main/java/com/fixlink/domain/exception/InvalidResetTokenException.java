package com.fixlink.domain.exception;

public class InvalidResetTokenException extends DomainException {
    public InvalidResetTokenException() {
        super("INVALID_RESET_TOKEN", "Liên kết đặt lại mật khẩu đã hết hạn hoặc không hợp lệ. Vui lòng gửi lại yêu cầu mới.", 400);
    }
}
