package com.bootsync.api;

import com.bootsync.auth.dto.SignupRequest;
import com.bootsync.config.AuthRateLimitService;
import com.bootsync.config.ClientIpResolver;
import com.bootsync.member.service.MemberSignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final ApiSessionResponseFactory sessionResponseFactory;
    private final AuthRateLimitService authRateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final MemberSignupService memberSignupService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ApiAuthController(
        ApiSessionResponseFactory sessionResponseFactory,
        AuthRateLimitService authRateLimitService,
        ClientIpResolver clientIpResolver,
        MemberSignupService memberSignupService,
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository
    ) {
        this.sessionResponseFactory = sessionResponseFactory;
        this.authRateLimitService = authRateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.memberSignupService = memberSignupService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/session")
    public ApiSessionResponse session(Authentication authentication, CsrfToken csrfToken) {
        return sessionResponseFactory.from(authentication, csrfToken);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody ApiAuthLoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        authRateLimitService.checkLoginAllowed(loginRequest.username(), clientIpResolver.resolve(request));

        try {
            Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    loginRequest.username().trim(),
                    loginRequest.password()
                )
            );
            authRateLimitService.clearLoginFailures(authentication.getName());
            establishSession(authentication, request, response);
            return ResponseEntity.noContent().build();
        } catch (DisabledException exception) {
            authRateLimitService.recordLoginFailure(loginRequest.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("inactive_account", "비활성 상태의 계정은 로그인할 수 없습니다."));
        } catch (BadCredentialsException exception) {
            authRateLimitService.recordLoginFailure(loginRequest.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("invalid_credentials", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
        @Valid @RequestBody SignupRequest signupRequest,
        Authentication authentication,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (isAuthenticated(authentication)) {
            return ResponseEntity.noContent().build();
        }

        authRateLimitService.checkSignupAllowed(clientIpResolver.resolve(request));

        if (memberSignupService.isUsernameTaken(signupRequest.username())) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.withField("validation_error", "이미 사용 중인 아이디입니다.", "username"));
        }
        if (memberSignupService.isRecoveryEmailTaken(signupRequest.recoveryEmail())) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.withField("validation_error", "이미 사용 중인 복구 이메일입니다.", "recoveryEmail"));
        }

        memberSignupService.signup(signupRequest);
        Authentication authenticated = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(signupRequest.username().trim(), signupRequest.password())
        );
        establishSession(authenticated, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        Authentication authentication,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        logoutHandler.logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void establishSession(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
