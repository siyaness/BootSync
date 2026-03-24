package com.bootsync.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class FrontendPageController {

    private static final Set<String> PUBLIC_APP_PATHS = Set.of(
        "/app/login",
        "/app/signup",
        "/app/verify-email"
    );

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
        String requestPath = requestPath(request);
        if (!isAuthenticated(authentication) && !isPublicAppPath(requestPath)) {
            return redirectToLogin(request, requestPath);
        }

        if (!frontendAssetAvailability.isAvailable()) {
            model.addAttribute("requestedPath", request.getRequestURI());
            model.addAttribute("authenticated", isAuthenticated(authentication));
            return "frontend/missing";
        }
        return "forward:/app/index.html";
    }

    private boolean isPublicAppPath(String requestPath) {
        return PUBLIC_APP_PATHS.contains(requestPath);
    }

    private String redirectToLogin(HttpServletRequest request, String requestPath) {
        String nextPath = requestPath.equals("/app") || requestPath.equals("/app/")
            ? "/"
            : requestPath.substring("/app".length());
        UriComponentsBuilder nextPathBuilder = UriComponentsBuilder.fromPath(nextPath);
        request.getParameterMap().forEach((name, values) -> {
            if (values == null || values.length == 0) {
                nextPathBuilder.queryParam(name);
                return;
            }
            nextPathBuilder.queryParam(name, (Object[]) values);
        });
        nextPath = nextPathBuilder.build().toUriString();

        return "redirect:/app/login?next=" + URLEncoder.encode(nextPath, StandardCharsets.UTF_8);
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
