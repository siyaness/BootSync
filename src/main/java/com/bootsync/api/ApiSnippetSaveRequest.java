package com.bootsync.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ApiSnippetSaveRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 20_000) String content,
    List<String> tags,
    String secretWarningToken
) {
}
