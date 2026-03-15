package com.bootsync.member.service;

public class RecoveryEmailChangeValidationException extends RuntimeException {

    private final String fieldName;

    public RecoveryEmailChangeValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
