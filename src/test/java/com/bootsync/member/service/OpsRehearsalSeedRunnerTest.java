package com.bootsync.member.service;

import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.repository.TagRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "app.operations.rehearsal-seed.enabled=true",
    "app.operations.rehearsal-seed.close-context-after-run=false"
})
@ActiveProfiles("test")
@DirtiesContext
class OpsRehearsalSeedRunnerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private SnippetTagRepository snippetTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;

    @Test
    void runnerSeedsTargetsForLocalOpsRehearsals() {
        Member resetTarget = memberRepository.findByUsername(OpsRehearsalSeedRunner.RESET_USERNAME).orElseThrow();
        Assertions.assertThat(resetTarget.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        Assertions.assertThat(resetTarget.getRecoveryEmailVerifiedAt()).isNotNull();

        Member purgeTarget = memberRepository.findByUsername(OpsRehearsalSeedRunner.PURGE_USERNAME).orElseThrow();
        Assertions.assertThat(purgeTarget.getStatus()).isEqualTo(MemberStatus.PENDING_DELETE);
        Assertions.assertThat(purgeTarget.getDeleteDueAt()).isNotNull();
        Assertions.assertThat(attendanceRecordRepository.countByMemberId(purgeTarget.getId())).isPositive();
        Assertions.assertThat(attendanceAuditLogRepository.countByMemberIdOrChangedByMemberId(purgeTarget.getId(), purgeTarget.getId()))
            .isPositive();
        Assertions.assertThat(snippetRepository.countByMemberId(purgeTarget.getId())).isPositive();
        Assertions.assertThat(snippetTagRepository.countByIdMemberId(purgeTarget.getId())).isPositive();
        Assertions.assertThat(tagRepository.countByMemberId(purgeTarget.getId())).isPositive();
        Assertions.assertThat(recoveryEmailVerificationTokenRepository.countByMemberId(purgeTarget.getId())).isPositive();

        Member scrubTarget = memberRepository.findByUsername(OpsRehearsalSeedRunner.SCRUB_USERNAME).orElseThrow();
        Assertions.assertThat(scrubTarget.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        Assertions.assertThat(attendanceRecordRepository.countByMemberId(scrubTarget.getId())).isPositive();
        Assertions.assertThat(attendanceAuditLogRepository.countByMemberIdOrChangedByMemberId(scrubTarget.getId(), scrubTarget.getId()))
            .isPositive();
        Assertions.assertThat(snippetRepository.countByMemberId(scrubTarget.getId())).isPositive();
        Assertions.assertThat(snippetTagRepository.countByIdMemberId(scrubTarget.getId())).isPositive();
        Assertions.assertThat(tagRepository.countByMemberId(scrubTarget.getId())).isPositive();
        Assertions.assertThat(recoveryEmailVerificationTokenRepository.countByMemberId(scrubTarget.getId())).isPositive();
    }
}
