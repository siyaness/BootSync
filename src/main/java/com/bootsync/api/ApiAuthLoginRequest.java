package com.bootsync.api;

import jakarta.validation.constraints.NotBlank;

public record ApiAuthLoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}
