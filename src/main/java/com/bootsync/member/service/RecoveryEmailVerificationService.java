package com.bootsync.member.service;

import com.bootsync.auth.dto.RecoveryEmailChangeRequest;
import com.bootsync.auth.dto.RecoveryEmailVerificationPreviewResponse;
import com.bootsync.auth.dto.RecoveryEmailVerificationResultResponse;
import com.bootsync.common.time.AppProperties;
import com.bootsync.member.dto.RecoveryEmailStatusView;
import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import com.bootsync.member.entity.RecoveryEmailVerificationToken;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryEmailVerificationService {

    private static final int TOKEN_TTL_MINUTES = 30;

    private final MemberRepository memberRepository;
    private final RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;
    private final RecoveryEmailDeliveryService recoveryEmailDeliveryService;
    private final RecoveryEmailVerificationPreviewStore previewStore;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AppProperties appProperties;

    public RecoveryEmailVerificationService(
        MemberRepository memberRepository,
        RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository,
        RecoveryEmailDeliveryService recoveryEmailDeliveryService,
        RecoveryEmailVerificationPreviewStore previewStore,
        PasswordEncoder passwordEncoder,
        Clock clock,
        AppProperties appProperties
    ) {
        this.memberRepository = memberRepository;
        this.recoveryEmailVerificationTokenRepository = recoveryEmailVerificationTokenRepository;
        this.recoveryEmailDeliveryService = recoveryEmailDeliveryService;
        this.previewStore = previewStore;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.appProperties = appProperties;
    }

    @Transactional
    public void issueSignupVerification(Member member, String recoveryEmail) {
        issueVerification(member, normalizeEmail(recoveryEmail), RecoveryEmailVerificationPurpose.SIGNUP_VERIFY);
    }

    @Transactional
    public void requestRecoveryEmailChange(String username, RecoveryEmailChangeRequest changeRequest) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + username));

        if (!passwordEncoder.matches(changeRequest.currentPassword(), member.getPasswordHash())) {
            throw new RecoveryEmailChangeValidationException("currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }

        String normalizedTargetEmail = normalizeEmail(changeRequest.newRecoveryEmail());
        if (normalizedTargetEmail.equalsIgnoreCase(member.getRecoveryEmail())) {
            throw new RecoveryEmailChangeValidationException("newRecoveryEmail", "이미 현재 복구 이메일로 사용 중입니다.");
        }

        Optional<Member> assignedMember = memberRepository.findByRecoveryEmailIgnoreCase(normalizedTargetEmail);
        if (assignedMember.isPresent() && !Objects.equals(assignedMember.get().getId(), member.getId())) {
            throw new RecoveryEmailChangeValidationException("newRecoveryEmail", "이미 사용 중인 복구 이메일입니다.");
        }

        issueVerification(member, normalizedTargetEmail, RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE);
    }

    @Transactional(readOnly = true)
    public RecoveryEmailVerificationPreviewResponse previewSignupVerification(String rawToken) {
        return previewVerification(rawToken, RecoveryEmailVerificationPurpose.SIGNUP_VERIFY, null);
    }

    @Transactional(readOnly = true)
    public RecoveryEmailVerificationPreviewResponse previewRecoveryEmailChange(String username, String rawToken) {
        Optional<Member> member = memberRepository.findByUsername(username);
        if (member.isEmpty()) {
            return invalidPreview(RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE);
        }
        return previewVerification(rawToken, RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE, member.get().getId());
    }

    @Transactional
    public RecoveryEmailVerificationResultResponse confirmSignupVerification(String rawToken) {
        return confirmVerification(rawToken, RecoveryEmailVerificationPurpose.SIGNUP_VERIFY, null);
    }

    @Transactional
    public RecoveryEmailVerificationResultResponse confirmRecoveryEmailChange(String username, String rawToken) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + username));
        return confirmVerification(rawToken, RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE, member.getId());
    }

    @Transactional
    public boolean resendLatestPendingVerification(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + username));

        Optional<RecoveryEmailVerificationToken> pendingToken = recoveryEmailVerificationTokenRepository
            .findTopByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(member.getId());

        if (pendingToken.isEmpty()) {
            return false;
        }

        issueVerification(member, pendingToken.get().getTargetEmail(), pendingToken.get().getPurpose());
        return true;
    }

    @Transactional(readOnly = true)
    public RecoveryEmailStatusView buildStatusFor(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + username));

        Optional<RecoveryEmailVerificationToken> pendingToken = recoveryEmailVerificationTokenRepository
            .findTopByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(member.getId());

        return new RecoveryEmailStatusView(
            member.getRecoveryEmail() != null && member.getRecoveryEmailVerifiedAt() != null,
            maskEmail(member.getRecoveryEmail()),
            pendingToken.isPresent(),
            pendingToken.map(RecoveryEmailVerificationToken::getTargetEmail).map(this::maskEmail).orElse(null),
            pendingToken.map(token -> purposeLabel(token.getPurpose())).orElse(null),
            pendingToken.map(RecoveryEmailVerificationToken::getExpiresAt).orElse(null),
            appProperties.getRecoveryEmail().isDevelopmentPreviewEnabled()
                ? pendingToken.flatMap(token -> previewStore.find(member.getId(), token.getPurpose()))
                    .map(RecoveryEmailVerificationPreviewLink::path)
                    .orElse(null)
                : null
        );
    }

    public String normalizeEmail(String recoveryEmail) {
        return recoveryEmail.trim().toLowerCase(Locale.ROOT);
    }

    private void issueVerification(Member member, String targetEmail, RecoveryEmailVerificationPurpose purpose) {
        LocalDateTime now = LocalDateTime.now(clock);
        invalidateUnusedTokensForMember(member.getId(), now);

        String rawToken = generateRawToken();
        RecoveryEmailVerificationToken token = new RecoveryEmailVerificationToken();
        token.setMemberId(member.getId());
        token.setPurpose(purpose);
        token.setTargetEmail(targetEmail);
        token.setTokenHash(hashToken(rawToken));
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusMinutes(TOKEN_TTL_MINUTES));

        recoveryEmailVerificationTokenRepository.save(token);
        recoveryEmailDeliveryService.deliver(member, purpose, targetEmail, rawToken);
    }

    private RecoveryEmailVerificationPreviewResponse previewVerification(
        String rawToken,
        RecoveryEmailVerificationPurpose purpose,
        Long expectedMemberId
    ) {
        Optional<RecoveryEmailVerificationToken> token = findToken(rawToken, purpose)
            .filter(verificationToken -> expectedMemberId == null || Objects.equals(expectedMemberId, verificationToken.getMemberId()));
        if (token.isEmpty()) {
            return invalidPreview(purpose);
        }

        RecoveryEmailVerificationToken verificationToken = token.get();
        boolean alreadyConsumed = verificationToken.getConsumedAt() != null;
        boolean invalid = !alreadyConsumed && isInvalidForUse(verificationToken, LocalDateTime.now(clock));
        return new RecoveryEmailVerificationPreviewResponse(
            verificationToken.getPurpose().name(),
            maskEmail(verificationToken.getTargetEmail()),
            verificationToken.getExpiresAt(),
            alreadyConsumed,
            invalid
        );
    }

    private RecoveryEmailVerificationResultResponse confirmVerification(
        String rawToken,
        RecoveryEmailVerificationPurpose purpose,
        Long expectedMemberId
    ) {
        Optional<RecoveryEmailVerificationToken> tokenOptional = findToken(rawToken, purpose)
            .filter(verificationToken -> expectedMemberId == null || Objects.equals(expectedMemberId, verificationToken.getMemberId()));
        if (tokenOptional.isEmpty()) {
            return invalidResult();
        }

        RecoveryEmailVerificationToken token = tokenOptional.get();
        LocalDateTime now = LocalDateTime.now(clock);
        Member member = memberRepository.findById(token.getMemberId()).orElse(null);
        if (member == null) {
            token.setInvalidatedAt(now);
            recoveryEmailVerificationTokenRepository.save(token);
            return new RecoveryEmailVerificationResultResponse(false, maskEmail(token.getTargetEmail()), "계정을 찾을 수 없습니다.");
        }

        if (token.getConsumedAt() != null) {
            return new RecoveryEmailVerificationResultResponse(true, maskEmail(token.getTargetEmail()), alreadyConsumedMessage(purpose));
        }

        if (isInvalidForUse(token, now)) {
            return invalidResult(maskEmail(token.getTargetEmail()));
        }

        String targetEmail = token.getTargetEmail();
        Optional<Member> assignedMember = memberRepository.findByRecoveryEmailIgnoreCase(targetEmail);
        if (assignedMember.isPresent() && !Objects.equals(assignedMember.get().getId(), member.getId())) {
            token.setInvalidatedAt(now);
            recoveryEmailVerificationTokenRepository.save(token);
            return new RecoveryEmailVerificationResultResponse(
                false,
                maskEmail(targetEmail),
                "이 복구 이메일은 이미 다른 계정에서 사용 중입니다. 설정에서 다시 시도해 주세요."
            );
        }

        member.setRecoveryEmail(targetEmail);
        member.setRecoveryEmailVerifiedAt(now);
        member.setUpdatedAt(now);
        memberRepository.save(member);

        token.setConsumedAt(now);
        recoveryEmailVerificationTokenRepository.save(token);
        invalidateUnusedTokensForMember(member.getId(), now);

        return new RecoveryEmailVerificationResultResponse(true, maskEmail(targetEmail), successMessage(purpose));
    }

    private Optional<RecoveryEmailVerificationToken> findToken(String rawToken, RecoveryEmailVerificationPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        return recoveryEmailVerificationTokenRepository.findByTokenHash(hashToken(rawToken))
            .filter(token -> token.getPurpose() == purpose);
    }

    private void invalidateUnusedTokensForMember(Long memberId, LocalDateTime invalidatedAt) {
        List<RecoveryEmailVerificationToken> activeTokens = recoveryEmailVerificationTokenRepository
            .findByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(memberId);

        for (RecoveryEmailVerificationToken activeToken : activeTokens) {
            activeToken.setInvalidatedAt(invalidatedAt);
        }

        recoveryEmailVerificationTokenRepository.saveAll(activeTokens);
    }

    private boolean isInvalidForUse(RecoveryEmailVerificationToken token, LocalDateTime now) {
        return token.getInvalidatedAt() != null || token.getExpiresAt().isBefore(now);
    }

    private RecoveryEmailVerificationPreviewResponse invalidPreview(RecoveryEmailVerificationPurpose purpose) {
        return new RecoveryEmailVerificationPreviewResponse(
            purpose.name(),
            null,
            null,
            false,
            true
        );
    }

    private RecoveryEmailVerificationResultResponse invalidResult() {
        return invalidResult(null);
    }

    private RecoveryEmailVerificationResultResponse invalidResult(String maskedTargetEmail) {
        return new RecoveryEmailVerificationResultResponse(false, maskedTargetEmail, "유효하지 않거나 만료된 인증 링크입니다.");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    private String alreadyConsumedMessage(RecoveryEmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_VERIFY -> "이미 인증이 완료된 링크입니다.";
            case RECOVERY_EMAIL_CHANGE -> "이미 복구 이메일 변경이 완료된 링크입니다.";
        };
    }

    private String successMessage(RecoveryEmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_VERIFY -> "복구 이메일 인증이 완료되었습니다.";
            case RECOVERY_EMAIL_CHANGE -> "복구 이메일 변경이 완료되었습니다.";
        };
    }

    private String purposeLabel(RecoveryEmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_VERIFY -> "회원가입 복구 이메일 인증";
            case RECOVERY_EMAIL_CHANGE -> "복구 이메일 변경 인증";
        };
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        return maskPart(local) + "@" + maskPart(domain);
    }

    private String maskPart(String part) {
        if (part.length() <= 1) {
            return "*";
        }
        if (part.length() == 2) {
            return part.charAt(0) + "*";
        }
        return part.substring(0, 2) + "*".repeat(part.length() - 2);
    }
}
