package com.bootsync.member.dto;

import java.time.LocalDateTime;

public record RecoveryEmailStatusView(
    boolean hasVerifiedRecoveryEmail,
    String maskedVerifiedRecoveryEmail,
    boolean hasPendingVerification,
    String maskedPendingRecoveryEmail,
    String pendingPurposeLabel,
    LocalDateTime pendingVerificationExpiresAt,
    String developmentPreviewPath
) {
}
