package com.bootsync.api;

import com.bootsync.attendance.service.AttendanceRequestValidationException;
import com.bootsync.config.RateLimitExceededException;
import com.bootsync.member.service.MemberValidationException;
import com.bootsync.member.service.RecoveryEmailChangeValidationException;
import com.bootsync.snippet.service.SnippetFormValidationException;
import com.bootsync.snippet.service.SnippetSecretWarningRequiredException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "com.bootsync.api")
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String message = fieldErrors.values().stream().findFirst().orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(new ApiErrorResponse("validation_error", message, fieldErrors));
    }

    @ExceptionHandler(MemberValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberValidation(MemberValidationException exception) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.withField("validation_error", exception.getMessage(), exception.getFieldName())
        );
    }

    @ExceptionHandler(RecoveryEmailChangeValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleRecoveryEmailChangeValidation(RecoveryEmailChangeValidationException exception) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.withField("validation_error", exception.getMessage(), exception.getFieldName())
        );
    }

    @ExceptionHandler(SnippetFormValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleSnippetValidation(SnippetFormValidationException exception) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.withField("validation_error", exception.getMessage(), exception.getFieldName())
        );
    }

    @ExceptionHandler(AttendanceRequestValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleAttendanceValidation(AttendanceRequestValidationException exception) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.withField("validation_error", exception.getMessage(), exception.getFieldName())
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiErrorResponse.of("rate_limit", exception.getMessage()));
    }

    @ExceptionHandler(SnippetSecretWarningRequiredException.class)
    public ResponseEntity<?> handleSnippetSecretWarning(SnippetSecretWarningRequiredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getWarningResponse());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null || exception.getReason().isBlank()
            ? "요청을 처리할 수 없습니다."
            : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
            .body(ApiErrorResponse.of(exception.getStatusCode().toString(), message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of("data_conflict", "저장 중 충돌이 발생했습니다. 다시 시도해 주세요."));
    }
}
