package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.settings.dto.PasswordChangeRequest;
import com.bootsync.settings.dto.ProfileUpdateRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSettingsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberPasswordPolicy memberPasswordPolicy;
    private final Clock clock;

    public MemberSettingsService(
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

    @Transactional(readOnly = true)
    public ProfileUpdateRequest profileFormFor(String username) {
        Member member = getRequiredMember(username);
        return new ProfileUpdateRequest(member.getDisplayName());
    }

    @Transactional
    public Member updateProfile(String username, ProfileUpdateRequest profileUpdateRequest) {
        Member member = getRequiredMember(username);
        String normalizedDisplayName = profileUpdateRequest.displayName().trim();

        if (normalizedDisplayName.length() < 2 || normalizedDisplayName.length() > 20) {
            throw new MemberValidationException("displayName", "표시 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다.");
        }

        member.setDisplayName(normalizedDisplayName);
        member.setUpdatedAt(LocalDateTime.now(clock));
        return memberRepository.save(member);
    }

    @Transactional
    public Member changePassword(String username, PasswordChangeRequest passwordChangeRequest) {
        Member member = getRequiredMember(username);

        if (!passwordEncoder.matches(passwordChangeRequest.currentPassword(), member.getPasswordHash())) {
            throw new MemberValidationException("currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }

        if (!passwordChangeRequest.newPassword().equals(passwordChangeRequest.newPasswordConfirm())) {
            throw new MemberValidationException("newPasswordConfirm", "새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(passwordChangeRequest.newPassword(), member.getPasswordHash())) {
            throw new MemberValidationException("newPassword", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        memberPasswordPolicy.validate(passwordChangeRequest.newPassword(), "newPassword");

        member.setPasswordHash(passwordEncoder.encode(passwordChangeRequest.newPassword()));
        member.setUpdatedAt(LocalDateTime.now(clock));
        return memberRepository.save(member);
    }

    public PasswordChangeRequest blankPasswordChangeForm() {
        return new PasswordChangeRequest("", "", "");
    }

    private Member getRequiredMember(String username) {
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다: " + username));
    }
}
