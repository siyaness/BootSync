package com.bootsync.member.security;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record BootSyncPrincipal(
    Long memberId,
    String username,
    String displayName,
    String passwordHash,
    MemberStatus status
) implements UserDetails {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    public static BootSyncPrincipal from(Member member) {
        return new BootSyncPrincipal(
            member.getId(),
            member.getUsername(),
            member.getDisplayName(),
            member.getPasswordHash(),
            member.getStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return status == MemberStatus.ACTIVE;
    }
}
