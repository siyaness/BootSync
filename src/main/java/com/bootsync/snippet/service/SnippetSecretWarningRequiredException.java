package com.bootsync.snippet.service;

import com.bootsync.snippet.dto.SnippetSecretWarningResponse;

public class SnippetSecretWarningRequiredException extends RuntimeException {

    private final SnippetSecretWarningResponse warningResponse;

    public SnippetSecretWarningRequiredException(SnippetSecretWarningResponse warningResponse) {
        super(warningResponse.message());
        this.warningResponse = warningResponse;
    }

    public SnippetSecretWarningResponse getWarningResponse() {
        return warningResponse;
    }
}
