package com.bootsync.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank
    String currentPassword,
    @NotBlank
    @Size(min = 10, max = 64)
    @Pattern(regexp = "^\\S+$")
    String newPassword,
    @NotBlank
    String newPasswordConfirm
) {
}
