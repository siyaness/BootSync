package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorPasswordResetService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberPasswordPolicy memberPasswordPolicy;
    private final Clock clock;

    public OperatorPasswordResetService(
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        MemberPasswordPolicy memberPasswordPolicy,
        Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberPasswordPolicy = memberPasswordPolicy;
        this.clock = clock;
    }

    @Transactional
    public OperatorPasswordResetResult resetPassword(String username, String temporaryPassword) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다: " + username));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new MemberValidationException(null, "ACTIVE 상태 계정만 운영자 보조 비밀번호 초기화를 진행할 수 있습니다.");
        }

        if (!hasVerifiedRecoveryEmail(member)) {
            throw new MemberValidationException(null, "검증된 복구 이메일이 있는 계정만 운영자 보조 비밀번호 초기화를 진행할 수 있습니다.");
        }

        if (passwordEncoder.matches(temporaryPassword, member.getPasswordHash())) {
            throw new MemberValidationException("temporaryPassword", "임시 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        memberPasswordPolicy.validate(temporaryPassword, "temporaryPassword");

        member.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        member.setUpdatedAt(LocalDateTime.now(clock));
        memberRepository.save(member);

        return new OperatorPasswordResetResult(
            member.getId(),
            member.getUsername(),
            maskEmail(member.getRecoveryEmail())
        );
    }

    private boolean hasVerifiedRecoveryEmail(Member member) {
        return member.getRecoveryEmail() != null
            && !member.getRecoveryEmail().isBlank()
            && member.getRecoveryEmailVerifiedAt() != null;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@", 2);
        return maskPart(parts[0]) + "@" + maskPart(parts[1]);
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

    public record OperatorPasswordResetResult(
        Long memberId,
        String username,
        String maskedRecoveryEmail
    ) {
    }
}
