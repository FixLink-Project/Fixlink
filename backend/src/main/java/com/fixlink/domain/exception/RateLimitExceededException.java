package com.fixlink.domain.exception;

public class RateLimitExceededException extends DomainException {
    public RateLimitExceededException() {
        super("RATE_LIMIT_EXCEEDED", "Bạn đã yêu cầu đặt lại mật khẩu quá số lần cho phép. Vui lòng thử lại sau 1 giờ.", 429);
    }
}
