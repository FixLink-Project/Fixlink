package com.fixlink.domain.exception;

import java.util.Map;

public class DomainException extends RuntimeException {
    private final String errorCode;
    private int statusCode = 400;
    private Map<String, String> errors;

    public DomainException(String message) {
        super(message);
        this.errorCode = "DOMAIN_ERROR";
    }

    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public DomainException(String errorCode, String message, Map<String, String> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    public DomainException(String errorCode, String message, int statusCode, Map<String, String> errors) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.errors = errors;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
