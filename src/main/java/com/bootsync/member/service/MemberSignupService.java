package com.bootsync.member.service;

import com.bootsync.auth.dto.SignupRequest;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSignupService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberPasswordPolicy memberPasswordPolicy;
    private final RecoveryEmailVerificationService recoveryEmailVerificationService;
    private final Clock clock;

    public MemberSignupService(
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        MemberPasswordPolicy memberPasswordPolicy,
        RecoveryEmailVerificationService recoveryEmailVerificationService,
        Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberPasswordPolicy = memberPasswordPolicy;
        this.recoveryEmailVerificationService = recoveryEmailVerificationService;
        this.clock = clock;
    }

    @Transactional
    public Member signup(SignupRequest signupRequest) {
        LocalDateTime now = LocalDateTime.now(clock);
        memberPasswordPolicy.validate(signupRequest.password(), "password");

        Member member = new Member();
        member.setUsername(signupRequest.username().trim());
        member.setPasswordHash(passwordEncoder.encode(signupRequest.password()));
        member.setDisplayName(signupRequest.displayName().trim());
        member.setStatus(MemberStatus.ACTIVE);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);

        Member savedMember = memberRepository.save(member);
        recoveryEmailVerificationService.issueSignupVerification(savedMember, signupRequest.recoveryEmail());
        return savedMember;
    }

    public boolean isUsernameTaken(String username) {
        return memberRepository.existsByUsername(username.trim());
    }

    public boolean isRecoveryEmailTaken(String recoveryEmail) {
        return memberRepository.existsByRecoveryEmailIgnoreCase(normalizeEmail(recoveryEmail));
    }

    public String normalizeEmail(String recoveryEmail) {
        return recoveryEmail.trim().toLowerCase(Locale.ROOT);
    }
}
