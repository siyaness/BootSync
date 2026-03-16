package com.bootsync.config;

import com.bootsync.common.time.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PrometheusScrapeTokenFilter extends OncePerRequestFilter {

    private static final String PROMETHEUS_PATH = "/actuator/prometheus";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AppProperties appProperties;

    public PrometheusScrapeTokenFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROMETHEUS_PATH.equals(requestPath(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String expectedToken = appProperties.getMonitoring().getPrometheusScrapeToken();

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            reject(response);
            return;
        }

        String actualToken = authorization.substring(BEARER_PREFIX.length());
        if (!tokensEqual(expectedToken, actualToken)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer realm=\"bootsync-prometheus\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().flush();
    }

    private boolean tokensEqual(String expectedToken, String actualToken) {
        return MessageDigest.isEqual(
            expectedToken.getBytes(StandardCharsets.UTF_8),
            actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
