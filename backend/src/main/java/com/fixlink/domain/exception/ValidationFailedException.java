package com.fixlink.domain.exception;

import java.util.Map;

public class ValidationFailedException extends DomainException {
    public ValidationFailedException(String message, Map<String, String> errors) {
        super("VALIDATION_FAILED", message, 400, errors);
    }
}
