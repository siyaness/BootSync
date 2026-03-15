package com.bootsync.api;

public record ApiUserResponse(
    String username,
    String displayName,
    String recoveryEmail,
    boolean emailVerified,
    String accountStatus,
    String deletionDate
) {
}
