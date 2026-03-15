package com.bootsync.api;

import java.time.LocalDateTime;

public record ApiAccountDeletionStatusResponse(
    boolean verifiedRecoveryEmailAvailable,
    boolean pendingDelete,
    LocalDateTime deleteRequestedAt,
    LocalDateTime deleteDueAt,
    boolean canRequestDeletion
) {
}
