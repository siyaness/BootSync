package com.bootsync.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendPageController {

    private final FrontendAssetAvailability frontendAssetAvailability;

    public FrontendPageController(FrontendAssetAvailability frontendAssetAvailability) {
        this.frontendAssetAvailability = frontendAssetAvailability;
    }

    @GetMapping({
        "/app",
        "/app/",
        "/app/login",
        "/app/signup",
        "/app/verify-email",
        "/app/dashboard",
        "/app/course-status",
        "/app/attendance",
        "/app/allowance",
        "/app/snippets",
        "/app/snippets/new",
        "/app/snippets/{snippetId}",
        "/app/snippets/{snippetId}/edit",
        "/app/settings"
    })
    public String app(HttpServletRequest request, Authentication authentication, Model model) {
        if (!frontendAssetAvailability.isAvailable()) {
            model.addAttribute("requestedPath", request.getRequestURI());
            model.addAttribute("authenticated", isAuthenticated(authentication));
            return "frontend/missing";
        }
        return "forward:/app/index.html";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
