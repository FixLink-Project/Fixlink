package com.fixlink.domain.exception;

import com.fixlink.domain.model.RequestStatus;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(RequestStatus current, RequestStatus expected) {
        super("INVALID_STATE_TRANSITION",
                String.format("Không thể chuyển trạng thái từ [%s] sang [%s]", current, expected));
    }
}
