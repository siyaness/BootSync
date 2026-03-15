package com.bootsync.snippet.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SnippetSearchItemResponse(
    Long id,
    String title,
    List<String> tags,
    LocalDateTime updatedAt,
    String highlightText
) {
}
