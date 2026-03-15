package com.bootsync.snippet.service;

public class SnippetFormValidationException extends RuntimeException {

    private final String fieldName;

    public SnippetFormValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
