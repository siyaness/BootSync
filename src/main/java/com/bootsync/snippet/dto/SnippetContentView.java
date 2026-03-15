package com.bootsync.snippet.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SnippetContentView(
    Long id,
    String title,
    String contentMarkdown,
    List<String> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
