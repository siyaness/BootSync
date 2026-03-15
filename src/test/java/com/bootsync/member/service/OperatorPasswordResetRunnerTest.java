package com.bootsync.member.service;

import com.bootsync.member.entity.Member;
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
    "app.operations.password-reset.enabled=true",
    "app.operations.password-reset.username=ops_runner_target",
    "app.operations.password-reset.temporary-password=runner-password-123",
    "app.operations.password-reset.actor=ops-admin",
    "app.operations.password-reset.reason=runbook-rehearsal",
    "app.operations.password-reset.close-context-after-run=false"
})
@ActiveProfiles("test")
@DirtiesContext
class OperatorPasswordResetRunnerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void runnerResetsPasswordForConfiguredUser() {
        Member targetMember = memberRepository.findByUsername("ops_runner_target").orElseThrow();

        Assertions.assertThat(passwordEncoder.matches("runner-password-123", targetMember.getPasswordHash())).isTrue();
        Assertions.assertThat(passwordEncoder.matches("before-password", targetMember.getPasswordHash())).isFalse();
    }

    @TestConfiguration
    static class VerifiedMemberConfig {

        @Bean
        @Order(20)
        ApplicationRunner operatorRunnerTargetInitializer(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
        ) {
            return args -> {
                if (memberRepository.findByUsername("ops_runner_target").isPresent()) {
                    return;
                }

                LocalDateTime now = LocalDateTime.now(clock);
                Member member = new Member();
                member.setUsername("ops_runner_target");
                member.setPasswordHash(passwordEncoder.encode("before-password"));
                member.setDisplayName("ops_runner_target");
                member.setRecoveryEmail("ops-runner@example.com");
                member.setRecoveryEmailVerifiedAt(now.minusHours(1));
                member.setCreatedAt(now.minusDays(1));
                member.setUpdatedAt(now.minusMinutes(10));
                memberRepository.save(member);
            };
        }
    }
}
