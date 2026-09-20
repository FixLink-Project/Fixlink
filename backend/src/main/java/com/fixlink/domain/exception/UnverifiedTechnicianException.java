package com.fixlink.domain.exception;

public class UnverifiedTechnicianException extends DomainException {
    public UnverifiedTechnicianException(String message) {
        super("UNVERIFIED_TECHNICIAN", message);
    }
}
