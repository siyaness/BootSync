package com.bootsync.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank
    @Size(min = 2, max = 20)
    String displayName
) {
}
