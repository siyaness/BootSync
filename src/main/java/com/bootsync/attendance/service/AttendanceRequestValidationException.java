package com.bootsync.attendance.service;

public class AttendanceRequestValidationException extends RuntimeException {

    private final String fieldName;

    public AttendanceRequestValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
