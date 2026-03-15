package com.bootsync.auth.dto;

import java.time.LocalDateTime;

public record RecoveryEmailVerificationPreviewResponse(
    String purpose,
    String maskedTargetEmail,
    LocalDateTime verificationExpiresAt,
    boolean alreadyConsumed,
    boolean invalid
) {
}
