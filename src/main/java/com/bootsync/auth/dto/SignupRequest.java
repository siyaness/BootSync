package com.bootsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[a-z0-9_]+$")
    String username,
    @NotBlank
    @Size(min = 10, max = 64)
    @Pattern(regexp = "^\\S+$")
    String password,
    @NotBlank
    @Size(min = 2, max = 20)
    String displayName,
    @NotBlank
    @Email
    @Size(max = 255)
    String recoveryEmail
) {
}
