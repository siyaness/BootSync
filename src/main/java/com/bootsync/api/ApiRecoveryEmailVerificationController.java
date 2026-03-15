package com.bootsync.api;

import com.bootsync.auth.dto.RecoveryEmailVerificationPreviewResponse;
import com.bootsync.auth.dto.RecoveryEmailVerificationResultResponse;
import com.bootsync.member.service.RecoveryEmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/recovery-email")
public class ApiRecoveryEmailVerificationController {

    private final RecoveryEmailVerificationService recoveryEmailVerificationService;

    public ApiRecoveryEmailVerificationController(RecoveryEmailVerificationService recoveryEmailVerificationService) {
        this.recoveryEmailVerificationService = recoveryEmailVerificationService;
    }

    @GetMapping("/preview")
    public RecoveryEmailVerificationPreviewResponse preview(
        @RequestParam String purpose,
        @RequestParam String token,
        Authentication authentication
    ) {
        return switch (normalizePurpose(purpose)) {
            case "signup" -> recoveryEmailVerificationService.previewSignupVerification(token);
            case "change" -> recoveryEmailVerificationService.previewRecoveryEmailChange(requireUsername(authentication), token);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 목적입니다.");
        };
    }

    @PostMapping("/confirm")
    public RecoveryEmailVerificationResultResponse confirm(
        @Valid @RequestBody ApiRecoveryEmailVerificationConfirmRequest request,
        Authentication authentication
    ) {
        return switch (normalizePurpose(request.purpose())) {
            case "signup" -> recoveryEmailVerificationService.confirmSignupVerification(request.token());
            case "change" -> recoveryEmailVerificationService.confirmRecoveryEmailChange(requireUsername(authentication), request.token());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 목적입니다.");
        };
    }

    private String requireUsername(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 후 복구 이메일 변경을 확인해 주세요.");
        }
        return authentication.getName();
    }

    private String normalizePurpose(String purpose) {
        if ("signup".equalsIgnoreCase(purpose)) {
            return "signup";
        }
        if ("change".equalsIgnoreCase(purpose)) {
            return "change";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 목적입니다.");
    }
}
