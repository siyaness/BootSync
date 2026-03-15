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
    "app.account-deletion.purge-run-once.enabled=true",
    "app.account-deletion.purge-run-once.actor=ops-admin",
    "app.account-deletion.purge-run-once.reason=runbook-rehearsal",
    "app.account-deletion.purge-run-once.close-context-after-run=false"
})
@ActiveProfiles("test")
@DirtiesContext
class AccountDeletionPurgeRunnerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void runnerPurgesOnlyDueMembers() {
        Assertions.assertThat(memberRepository.findByUsername("purge_runner_due")).isEmpty();
        Assertions.assertThat(memberRepository.findByUsername("purge_runner_future")).isPresent();
    }

    @TestConfiguration
    static class PendingDeleteMemberConfig {

        @Bean
        @Order(20)
        ApplicationRunner purgeRunnerTargetInitializer(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
        ) {
            return args -> {
                if (memberRepository.findByUsername("purge_runner_due").isEmpty()) {
                    memberRepository.save(createPendingDeleteMember(
                        "purge_runner_due",
                        passwordEncoder,
                        clock,
                        LocalDateTime.now(clock).minusHours(2)
                    ));
                }

                if (memberRepository.findByUsername("purge_runner_future").isEmpty()) {
                    memberRepository.save(createPendingDeleteMember(
                        "purge_runner_future",
                        passwordEncoder,
                        clock,
                        LocalDateTime.now(clock).plusDays(3)
                    ));
                }
            };
        }

        private Member createPendingDeleteMember(
            String username,
            PasswordEncoder passwordEncoder,
            Clock clock,
            LocalDateTime deleteDueAt
        ) {
            LocalDateTime now = LocalDateTime.now(clock);
            Member member = new Member();
            member.setUsername(username);
            member.setPasswordHash(passwordEncoder.encode("delete-password"));
            member.setDisplayName(username);
            member.setRecoveryEmail(username + "@example.com");
            member.setRecoveryEmailVerifiedAt(now.minusDays(5));
            member.setStatus(MemberStatus.PENDING_DELETE);
            member.setDeleteRequestedAt(deleteDueAt.minusDays(7));
            member.setDeleteDueAt(deleteDueAt);
            member.setCreatedAt(now.minusDays(10));
            member.setUpdatedAt(now.minusMinutes(10));
            return member;
        }
    }
}
