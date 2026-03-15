package com.bootsync.api;

import com.bootsync.member.dto.RecoveryEmailStatusView;
import com.bootsync.member.security.BootSyncPrincipal;
import com.bootsync.member.service.AccountDeletionService;
import com.bootsync.member.service.RecoveryEmailVerificationService;
import com.bootsync.settings.dto.AccountDeletionStatusView;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Component
public class ApiSessionResponseFactory {

    private final RecoveryEmailVerificationService recoveryEmailVerificationService;
    private final AccountDeletionService accountDeletionService;

    public ApiSessionResponseFactory(
        RecoveryEmailVerificationService recoveryEmailVerificationService,
        AccountDeletionService accountDeletionService
    ) {
        this.recoveryEmailVerificationService = recoveryEmailVerificationService;
        this.accountDeletionService = accountDeletionService;
    }

    public ApiSessionResponse from(Authentication authentication, CsrfToken csrfToken) {
        if (!isAuthenticated(authentication)) {
            return new ApiSessionResponse(false, csrf(csrfToken), null, null, null);
        }

        String username = authentication.getName();
        String displayName = username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof BootSyncPrincipal bootSyncPrincipal) {
            displayName = bootSyncPrincipal.displayName();
        }

        RecoveryEmailStatusView recoveryEmailStatus = recoveryEmailVerificationService.buildStatusFor(username);
        AccountDeletionStatusView accountDeletionStatus = accountDeletionService.statusFor(username);

        String recoveryEmail = recoveryEmailStatus.hasVerifiedRecoveryEmail()
            ? recoveryEmailStatus.maskedVerifiedRecoveryEmail()
            : recoveryEmailStatus.maskedPendingRecoveryEmail();

        return new ApiSessionResponse(
            true,
            csrf(csrfToken),
            new ApiUserResponse(
                username,
                displayName,
                recoveryEmail,
                recoveryEmailStatus.hasVerifiedRecoveryEmail(),
                accountDeletionStatus.pendingDelete() ? "pending_deletion" : "active",
                toDateString(accountDeletionStatus.deleteDueAt())
            ),
            new ApiRecoveryEmailStatusResponse(
                recoveryEmailStatus.hasVerifiedRecoveryEmail(),
                recoveryEmailStatus.maskedVerifiedRecoveryEmail(),
                recoveryEmailStatus.hasPendingVerification(),
                recoveryEmailStatus.maskedPendingRecoveryEmail(),
                recoveryEmailStatus.pendingPurposeLabel(),
                recoveryEmailStatus.pendingVerificationExpiresAt(),
                recoveryEmailStatus.developmentPreviewPath()
            ),
            new ApiAccountDeletionStatusResponse(
                accountDeletionStatus.verifiedRecoveryEmailAvailable(),
                accountDeletionStatus.pendingDelete(),
                accountDeletionStatus.deleteRequestedAt(),
                accountDeletionStatus.deleteDueAt(),
                accountDeletionStatus.canRequestDeletion()
            )
        );
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private ApiCsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new ApiCsrfTokenResponse(
            csrfToken.getHeaderName(),
            csrfToken.getParameterName(),
            csrfToken.getToken()
        );
    }

    private String toDateString(LocalDateTime value) {
        return value == null ? null : value.toLocalDate().toString();
    }
}
