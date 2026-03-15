package com.bootsync.api;

import java.time.LocalDateTime;

public record ApiRecoveryEmailStatusResponse(
    boolean hasVerifiedRecoveryEmail,
    String maskedVerifiedRecoveryEmail,
    boolean hasPendingVerification,
    String maskedPendingRecoveryEmail,
    String pendingPurposeLabel,
    LocalDateTime pendingVerificationExpiresAt,
    String developmentPreviewPath
) {
}
