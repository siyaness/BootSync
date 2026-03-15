package com.bootsync.api;

import java.util.Map;

public record ApiErrorResponse(
    String code,
    String message,
    Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, Map.of());
    }

    public static ApiErrorResponse withField(String code, String message, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return of(code, message);
        }
        return new ApiErrorResponse(code, message, Map.of(fieldName, message));
    }
}
