package com.fixlink.exception;

public class InvalidOperationException extends RuntimeException {

    private final String errorCode;

    public InvalidOperationException(String message) {
        super(message);
        this.errorCode = "INVALID_OPERATION";
    }

    public InvalidOperationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
