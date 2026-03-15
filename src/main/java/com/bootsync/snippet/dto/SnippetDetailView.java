package com.bootsync.snippet.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SnippetDetailView(
    Long id,
    String title,
    String renderedContentHtml,
    List<String> tags,
    LocalDateTime updatedAt
) {
}
