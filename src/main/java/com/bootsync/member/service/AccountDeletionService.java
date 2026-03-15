package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.settings.dto.AccountDeletionRequest;
import com.bootsync.settings.dto.AccountDeletionStatusView;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {

    private static final long DELETE_GRACE_PERIOD_DAYS = 7L;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountDeletionService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountDeletionStatusView statusFor(String username) {
        Member member = getRequiredMember(username);
        return new AccountDeletionStatusView(
            hasVerifiedRecoveryEmail(member),
            member.getStatus() == MemberStatus.PENDING_DELETE,
            member.getDeleteRequestedAt(),
            member.getDeleteDueAt()
        );
    }

    @Transactional
    public Member requestDeletion(String username, AccountDeletionRequest accountDeletionRequest) {
        Member member = getRequiredMember(username);

        if (member.getStatus() == MemberStatus.PENDING_DELETE) {
            throw new MemberValidationException(null, "이미 삭제 요청이 접수된 계정입니다.");
        }

        if (!hasVerifiedRecoveryEmail(member)) {
            throw new MemberValidationException(null, "검증된 복구 이메일이 있는 계정만 삭제 요청을 접수할 수 있습니다.");
        }

        if (!passwordEncoder.matches(accountDeletionRequest.currentPassword(), member.getPasswordHash())) {
            throw new MemberValidationException("currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        member.setStatus(MemberStatus.PENDING_DELETE);
        member.setDeleteRequestedAt(now);
        member.setDeleteDueAt(now.plusDays(DELETE_GRACE_PERIOD_DAYS));
        member.setUpdatedAt(now);
        return memberRepository.save(member);
    }

    @Transactional
    public Member cancelDeletion(String username) {
        Member member = getRequiredMember(username);

        if (member.getStatus() != MemberStatus.PENDING_DELETE) {
            throw new MemberValidationException(null, "삭제 요청 상태의 계정만 복구할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (member.getDeleteDueAt() == null || !member.getDeleteDueAt().isAfter(now)) {
            throw new MemberValidationException(null, "삭제 유예 기간이 지난 계정은 취소할 수 없습니다.");
        }

        member.setStatus(MemberStatus.ACTIVE);
        member.setDeleteRequestedAt(null);
        member.setDeleteDueAt(null);
        member.setUpdatedAt(now);
        return memberRepository.save(member);
    }

    private boolean hasVerifiedRecoveryEmail(Member member) {
        return member.getRecoveryEmail() != null
            && !member.getRecoveryEmail().isBlank()
            && member.getRecoveryEmailVerifiedAt() != null;
    }

    private Member getRequiredMember(String username) {
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다: " + username));
    }
}
