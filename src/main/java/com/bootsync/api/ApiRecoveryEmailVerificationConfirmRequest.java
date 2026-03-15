package com.bootsync.api;

import jakarta.validation.constraints.NotBlank;

public record ApiRecoveryEmailVerificationConfirmRequest(
    @NotBlank String purpose,
    @NotBlank String token
) {
}
