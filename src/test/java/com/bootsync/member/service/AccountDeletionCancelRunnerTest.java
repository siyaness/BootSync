package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "app.operations.deletion-cancel.enabled=true",
    "app.operations.deletion-cancel.username=ops_delete_cancel",
    "app.operations.deletion-cancel.actor=ops-admin",
    "app.operations.deletion-cancel.reason=runbook-rehearsal",
    "app.operations.deletion-cancel.close-context-after-run=false"
})
@ActiveProfiles("test")
@DirtiesContext
class AccountDeletionCancelRunnerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void runnerCancelsDeletionForConfiguredUser() {
        Member targetMember = memberRepository.findByUsername("ops_delete_cancel").orElseThrow();

        Assertions.assertThat(targetMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        Assertions.assertThat(targetMember.getDeleteRequestedAt()).isNull();
        Assertions.assertThat(targetMember.getDeleteDueAt()).isNull();
    }

    @TestConfiguration
    static class PendingDeleteMemberConfig {

        @Bean
        @Order(20)
        ApplicationRunner deleteCancelRunnerTargetInitializer(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
        ) {
            return args -> {
                if (memberRepository.findByUsername("ops_delete_cancel").isPresent()) {
                    return;
                }

                LocalDateTime now = LocalDateTime.now(clock);
                Member member = new Member();
                member.setUsername("ops_delete_cancel");
                member.setPasswordHash(passwordEncoder.encode("before-password"));
                member.setDisplayName("ops_delete_cancel");
                member.setRecoveryEmail("ops-delete-cancel@example.com");
                member.setRecoveryEmailVerifiedAt(now.minusHours(2));
                member.setStatus(MemberStatus.PENDING_DELETE);
                member.setDeleteRequestedAt(now.minusHours(1));
                member.setDeleteDueAt(now.plusDays(6));
                member.setCreatedAt(now.minusDays(3));
                member.setUpdatedAt(now.minusMinutes(10));
                memberRepository.save(member);
            };
        }
    }
}
