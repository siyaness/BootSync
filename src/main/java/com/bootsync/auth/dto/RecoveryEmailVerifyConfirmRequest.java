package com.bootsync.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RecoveryEmailVerifyConfirmRequest(@NotBlank String token) {
}
