package com.bootsync.common.controller;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class FrontendAssetAvailability {

    private final ResourceLoader resourceLoader;

    public FrontendAssetAvailability(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public boolean isAvailable() {
        return resourceLoader.getResource("classpath:/static/app/index.html").exists();
    }
}
