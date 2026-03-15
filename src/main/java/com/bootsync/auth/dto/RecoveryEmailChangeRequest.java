package com.bootsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoveryEmailChangeRequest(
    @NotBlank @Email @Size(max = 255) String newRecoveryEmail,
    @NotBlank @Size(min = 10, max = 64) String currentPassword
) {
}
