package com.bootsync.auth.dto;

public record RecoveryEmailVerificationResultResponse(
    boolean verified,
    String maskedRecoveryEmail,
    String message
) {
}
