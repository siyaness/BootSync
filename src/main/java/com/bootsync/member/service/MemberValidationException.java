package com.bootsync.member.service;

public class MemberValidationException extends RuntimeException {

    private final String fieldName;

    public MemberValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
