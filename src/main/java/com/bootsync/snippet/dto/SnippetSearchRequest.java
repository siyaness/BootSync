package com.bootsync.snippet.dto;

import jakarta.validation.constraints.Size;

public record SnippetSearchRequest(
    @Size(max = 100) String q,
    @Size(max = 20) String tag
) {
}
