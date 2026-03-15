package com.bootsync.api;

public record ApiCsrfTokenResponse(
    String headerName,
    String parameterName,
    String token
) {
}
