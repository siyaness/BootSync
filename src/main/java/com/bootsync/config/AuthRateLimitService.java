package com.bootsync.config;

import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

    private static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(10);
    private static final Duration LOGIN_USERNAME_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration SIGNUP_IP_WINDOW = Duration.ofHours(1);
    private static final Duration RECOVERY_HOURLY_WINDOW = Duration.ofHours(1);
    private static final Duration RECOVERY_RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final InMemoryRateLimitService rateLimitService;

    public AuthRateLimitService(InMemoryRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    public void checkLoginAllowed(String username, String clientIp) {
        if (rateLimitService.isExceeded(loginIpKey(clientIp), 10, LOGIN_IP_WINDOW)) {
            throw new RateLimitExceededException("로그인 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (username != null && !username.isBlank()
            && rateLimitService.isExceeded(loginFailureKey(username), 5, LOGIN_USERNAME_FAILURE_WINDOW)) {
            throw new RateLimitExceededException("로그인 실패 횟수가 많아 잠시 동안 차단되었습니다.");
        }
        rateLimitService.record(loginIpKey(clientIp));
    }

    public void recordLoginFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        rateLimitService.record(loginFailureKey(username));
    }

    public void clearLoginFailures(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        rateLimitService.clear(loginFailureKey(username));
    }

    public void checkSignupAllowed(String clientIp) {
        if (!rateLimitService.tryConsume(signupIpKey(clientIp), 5, SIGNUP_IP_WINDOW)) {
            throw new RateLimitExceededException("회원가입 요청이 너무 많습니다. 1시간 후 다시 시도해 주세요.");
        }
    }

    public void checkRecoveryEmailChangeAllowed(String username, String clientIp) {
        if (!rateLimitService.tryConsume(recoveryAccountKey("change", username), 5, RECOVERY_HOURLY_WINDOW)) {
            throw new RateLimitExceededException("복구 이메일 변경 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (!rateLimitService.tryConsume(recoveryIpKey("change", clientIp), 10, RECOVERY_HOURLY_WINDOW)) {
            throw new RateLimitExceededException("현재 IP에서 복구 이메일 변경 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public void checkRecoveryEmailResendAllowed(String username, String clientIp) {
        if (!rateLimitService.tryConsume(recoveryCooldownKey(username), 1, RECOVERY_RESEND_COOLDOWN)) {
            throw new RateLimitExceededException("복구 이메일 재발송은 1분 뒤에 다시 시도할 수 있습니다.");
        }
        if (!rateLimitService.tryConsume(recoveryAccountKey("resend", username), 5, RECOVERY_HOURLY_WINDOW)) {
            throw new RateLimitExceededException("복구 이메일 재발송 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (!rateLimitService.tryConsume(recoveryIpKey("resend", clientIp), 10, RECOVERY_HOURLY_WINDOW)) {
            throw new RateLimitExceededException("현재 IP에서 복구 이메일 재발송 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String loginIpKey(String clientIp) {
        return "login:ip:" + clientIp;
    }

    private String loginFailureKey(String username) {
        return "login:failure:" + username.trim().toLowerCase();
    }

    private String signupIpKey(String clientIp) {
        return "signup:ip:" + clientIp;
    }

    private String recoveryAccountKey(String action, String username) {
        return "recovery:" + action + ":account:" + username.trim().toLowerCase();
    }

    private String recoveryIpKey(String action, String clientIp) {
        return "recovery:" + action + ":ip:" + clientIp;
    }

    private String recoveryCooldownKey(String username) {
        return "recovery:resend:cooldown:" + username.trim().toLowerCase();
    }
}
