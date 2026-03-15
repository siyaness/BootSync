package com.bootsync.settings.dto;

import java.time.LocalDateTime;

public record AccountDeletionStatusView(
    boolean verifiedRecoveryEmailAvailable,
    boolean pendingDelete,
    LocalDateTime deleteRequestedAt,
    LocalDateTime deleteDueAt
) {
    public boolean canRequestDeletion() {
        return verifiedRecoveryEmailAvailable && !pendingDelete;
    }
}
