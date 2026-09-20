package com.fixlink.domain.exception;

import java.util.Map;

public class DuplicateCategoryNameException extends DomainException {
    public DuplicateCategoryNameException(String message, Map<String, String> errors) {
        super("DUPLICATE_CATEGORY_NAME", message, 400, errors);
    }
}
