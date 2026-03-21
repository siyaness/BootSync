package com.bootsync.config;

import com.bootsync.api.ApiErrorResponse;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.security.BootSyncPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ActiveMemberSessionFilter extends OncePerRequestFilter {

    public static final String ACTIVE_MEMBER_REVALIDATION_MARKER = "BOOTSYNC_ACTIVE_MEMBER_REVALIDATION";

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/",
        "/login",
        "/signup",
        "/auth/login",
        "/auth/recovery-email/verify",
        "/actuator/health",
        "/actuator/prometheus"
    );

    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ActiveMemberSessionFilter(MemberRepository memberRepository, ObjectMapper objectMapper) {
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!hasRevalidationMarker(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        SessionInvalidation sessionInvalidation = resolveInvalidation(authentication);
        if (sessionInvalidation != null) {
            logoutHandler.logout(request, response, authentication);
            SecurityContextHolder.clearContext();
            if (isApiRequest(request)) {
                writeApiUnauthorized(response, sessionInvalidation);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/app/login?reason=" + sessionInvalidation.reason());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private SessionInvalidation resolveInvalidation(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof BootSyncPrincipal bootSyncPrincipal)) {
            return null;
        }

        return memberRepository.findById(bootSyncPrincipal.memberId())
            .map(member -> {
                if (member.getStatus() == MemberStatus.PENDING_DELETE) {
                    return new SessionInvalidation("pending_delete", "계정 삭제 요청이 접수되어 현재 세션이 종료되었습니다.");
                }
                if (member.getStatus() != MemberStatus.ACTIVE) {
                    return new SessionInvalidation("inactive_account", "비활성화된 계정은 다시 로그인할 수 없습니다.");
                }
                if (!credentialsCurrent(authentication, member)) {
                    return new SessionInvalidation("session_expired", "보안을 위해 다시 로그인해 주세요.");
                }
                return null;
            })
            .orElse(new SessionInvalidation("session_expired", "회원 상태를 다시 확인하는 중 세션이 종료되었습니다."));
    }

    private boolean credentialsCurrent(Authentication authentication, Member member) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof BootSyncPrincipal bootSyncPrincipal) {
            return Objects.equals(bootSyncPrincipal.passwordHash(), member.getPasswordHash());
        }
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String requestPath = requestPath(request);
        return PUBLIC_PATHS.contains(requestPath)
            || requestPath.startsWith("/actuator/health")
            || requestPath.startsWith("/css/")
            || requestPath.startsWith("/js/")
            || requestPath.startsWith("/images/")
            || requestPath.startsWith("/webjars/")
            || requestPath.equals("/favicon.ico");
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return requestPath(request).startsWith("/api/");
    }

    private void writeApiUnauthorized(HttpServletResponse response, SessionInvalidation sessionInvalidation) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getWriter(),
            ApiErrorResponse.of(sessionInvalidation.reason(), sessionInvalidation.message())
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

    private boolean hasRevalidationMarker(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null
            && Boolean.TRUE.equals(session.getAttribute(ACTIVE_MEMBER_REVALIDATION_MARKER));
    }

    private record SessionInvalidation(String reason, String message) {
    }
}
