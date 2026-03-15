package com.bootsync.api;

import java.time.LocalDateTime;
import java.util.List;

public record ApiSnippetResponse(
    Long id,
    String title,
    String content,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
