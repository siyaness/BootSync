package com.bootsync.config;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.security.BootSyncPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ActiveMemberSessionFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/",
        "/login",
        "/signup",
        "/auth/login",
        "/auth/recovery-email/verify",
        "/actuator/health"
    );

    private final MemberRepository memberRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ActiveMemberSessionFilter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
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

        boolean active = memberRepository.findByUsername(authentication.getName())
            .map(member -> member.getStatus() == MemberStatus.ACTIVE && credentialsCurrent(authentication, member))
            .orElse(false);

        if (!active) {
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect(request.getContextPath() + "/app/login?inactive");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean credentialsCurrent(Authentication authentication, Member member) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof BootSyncPrincipal bootSyncPrincipal) {
            return Objects.equals(bootSyncPrincipal.passwordHash(), member.getPasswordHash());
        }
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return PUBLIC_PATHS.contains(requestUri)
            || requestUri.startsWith("/css/")
            || requestUri.startsWith("/js/")
            || requestUri.startsWith("/images/")
            || requestUri.startsWith("/webjars/")
            || requestUri.equals("/favicon.ico");
    }
}
