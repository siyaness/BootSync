package com.bootsync.snippet.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SnippetSecretWarningResponse(
    List<String> warningCodes,
    String message,
    boolean requiresAcknowledgement,
    String secretWarningToken,
    String contentFingerprint,
    LocalDateTime expiresAt
) {
}
