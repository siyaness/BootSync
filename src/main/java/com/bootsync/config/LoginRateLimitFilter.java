package com.bootsync.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimitService authRateLimitService;
    private final ClientIpResolver clientIpResolver;

    public LoginRateLimitFilter(AuthRateLimitService authRateLimitService, ClientIpResolver clientIpResolver) {
        this.authRateLimitService = authRateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!matchesLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authRateLimitService.checkLoginAllowed(request.getParameter("username"), clientIpResolver.resolve(request));
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exception) {
            response.sendRedirect(request.getContextPath() + "/app/login?rateLimit");
        }
    }

    private boolean matchesLoginRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)
            ? requestUri.substring(contextPath.length())
            : requestUri;
        return "POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(path);
    }
}
