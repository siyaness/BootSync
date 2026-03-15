package com.bootsync.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeletionRequest(
    @NotBlank
    String currentPassword
) {
}
