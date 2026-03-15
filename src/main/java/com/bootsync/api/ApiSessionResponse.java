package com.bootsync.api;

public record ApiSessionResponse(
    boolean authenticated,
    ApiCsrfTokenResponse csrf,
    ApiUserResponse user,
    ApiRecoveryEmailStatusResponse recoveryEmailStatus,
    ApiAccountDeletionStatusResponse accountDeletionStatus
) {
}
