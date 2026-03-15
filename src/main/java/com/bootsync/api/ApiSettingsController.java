package com.bootsync.api;

import com.bootsync.auth.dto.RecoveryEmailChangeRequest;
import com.bootsync.config.AuthRateLimitService;
import com.bootsync.config.ClientIpResolver;
import com.bootsync.member.entity.Member;
import com.bootsync.member.security.BootSyncPrincipal;
import com.bootsync.member.service.AccountDeletionService;
import com.bootsync.member.service.MemberSettingsService;
import com.bootsync.member.service.MemberTrainingProfileService;
import com.bootsync.member.service.RecoveryEmailVerificationService;
import com.bootsync.settings.dto.AccountDeletionRequest;
import com.bootsync.settings.dto.PasswordChangeRequest;
import com.bootsync.settings.dto.ProfileUpdateRequest;
import com.bootsync.settings.dto.TrainingProfileRequest;
import com.bootsync.settings.dto.TrainingProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/settings")
public class ApiSettingsController {

    private final AuthRateLimitService authRateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final MemberSettingsService memberSettingsService;
    private final MemberTrainingProfileService memberTrainingProfileService;
    private final RecoveryEmailVerificationService recoveryEmailVerificationService;
    private final AccountDeletionService accountDeletionService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ApiSettingsController(
        AuthRateLimitService authRateLimitService,
        ClientIpResolver clientIpResolver,
        MemberSettingsService memberSettingsService,
        MemberTrainingProfileService memberTrainingProfileService,
        RecoveryEmailVerificationService recoveryEmailVerificationService,
        AccountDeletionService accountDeletionService,
        SecurityContextRepository securityContextRepository
    ) {
        this.authRateLimitService = authRateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.memberSettingsService = memberSettingsService;
        this.memberTrainingProfileService = memberTrainingProfileService;
        this.recoveryEmailVerificationService = recoveryEmailVerificationService;
        this.accountDeletionService = accountDeletionService;
        this.securityContextRepository = securityContextRepository;
    }

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
        Authentication authentication,
        @Valid @RequestBody ProfileUpdateRequest profileUpdateRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Member updatedMember = memberSettingsService.updateProfile(authentication.getName(), profileUpdateRequest);
        refreshAuthentication(authentication, updatedMember, request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
        Authentication authentication,
        @Valid @RequestBody PasswordChangeRequest passwordChangeRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Member updatedMember = memberSettingsService.changePassword(authentication.getName(), passwordChangeRequest);
        refreshAuthentication(authentication, updatedMember, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/training-profile")
    public TrainingProfileResponse trainingProfile(Authentication authentication) {
        return memberTrainingProfileService.profileFor(authentication.getName());
    }

    @PutMapping("/training-profile")
    public TrainingProfileResponse updateTrainingProfile(
        Authentication authentication,
        @Valid @RequestBody TrainingProfileRequest trainingProfileRequest
    ) {
        return memberTrainingProfileService.updateProfile(authentication.getName(), trainingProfileRequest);
    }

    @DeleteMapping("/training-profile")
    public ResponseEntity<Void> clearTrainingProfile(Authentication authentication) {
        memberTrainingProfileService.clearProfile(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recovery-email")
    public ResponseEntity<Void> changeRecoveryEmail(
        Authentication authentication,
        @Valid @RequestBody RecoveryEmailChangeRequest changeRequest,
        HttpServletRequest request
    ) {
        authRateLimitService.checkRecoveryEmailChangeAllowed(authentication.getName(), clientIpResolver.resolve(request));
        recoveryEmailVerificationService.requestRecoveryEmailChange(authentication.getName(), changeRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recovery-email/resend")
    public ResponseEntity<Void> resendRecoveryEmail(
        Authentication authentication,
        HttpServletRequest request
    ) {
        authRateLimitService.checkRecoveryEmailResendAllowed(authentication.getName(), clientIpResolver.resolve(request));
        boolean resent = recoveryEmailVerificationService.resendLatestPendingVerification(authentication.getName());
        if (!resent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 재발송할 복구 이메일 인증 대상이 없습니다.");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/account-deletion")
    public ResponseEntity<Void> requestAccountDeletion(
        Authentication authentication,
        @Valid @RequestBody AccountDeletionRequest accountDeletionRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        accountDeletionService.requestDeletion(authentication.getName(), accountDeletionRequest);
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private void refreshAuthentication(
        Authentication authentication,
        Member updatedMember,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        BootSyncPrincipal updatedPrincipal = BootSyncPrincipal.from(updatedMember);
        UsernamePasswordAuthenticationToken refreshedAuthentication = new UsernamePasswordAuthenticationToken(
            updatedPrincipal,
            authentication.getCredentials(),
            authentication.getAuthorities()
        );
        refreshedAuthentication.setDetails(authentication.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(refreshedAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
