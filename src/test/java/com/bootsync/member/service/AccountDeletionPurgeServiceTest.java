package com.bootsync.member.service;

import com.bootsync.attendance.entity.AttendanceAuditAction;
import com.bootsync.attendance.entity.AttendanceAuditLog;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.entity.MemberTrainingProfile;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import com.bootsync.member.entity.RecoveryEmailVerificationToken;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.MemberTrainingProfileRepository;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.entity.Tag;
import com.bootsync.tag.repository.TagRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountDeletionPurgeServiceTest {

    @Autowired
    private AccountDeletionPurgeService accountDeletionPurgeService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private SnippetTagRepository snippetTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private MemberTrainingProfileRepository memberTrainingProfileRepository;

    @Autowired
    private RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void purgeDueMembersDeletesOnlyDueMembersAndAnonymizesAuditLogs() {
        Member dueMember = createPendingDeleteMember("purge_due_user", LocalDateTime.now(clock).minusDays(1));
        Member futureMember = createPendingDeleteMember("purge_future", LocalDateTime.now(clock).plusDays(2));

        Tag dueTag = tagRepository.save(createTag(dueMember.getId(), "purge-tag"));
        Snippet dueSnippet = snippetRepository.save(createSnippet(dueMember.getId(), "purge snippet", "purge content"));
        snippetTagRepository.save(link(dueMember.getId(), dueSnippet.getId(), dueTag.getId()));
        AttendanceRecord dueRecord = attendanceRecordRepository.save(
            createAttendanceRecord(dueMember.getId(), LocalDate.now(clock).minusDays(2), AttendanceStatus.ABSENT, "purge memo")
        );
        memberTrainingProfileRepository.save(createTrainingProfile(dueMember.getId()));
        recoveryEmailVerificationTokenRepository.save(createToken(dueMember.getId(), "purge@example.com"));
        AttendanceAuditLog dueLog = attendanceAuditLogRepository.save(createAuditLog(dueMember.getId(), dueRecord.getId()));

        Tag futureTag = tagRepository.save(createTag(futureMember.getId(), "future-tag"));
        Snippet futureSnippet = snippetRepository.save(createSnippet(futureMember.getId(), "future snippet", "future content"));
        snippetTagRepository.save(link(futureMember.getId(), futureSnippet.getId(), futureTag.getId()));
        AttendanceRecord futureRecord = attendanceRecordRepository.save(
            createAttendanceRecord(futureMember.getId(), LocalDate.now(clock).minusDays(1), AttendanceStatus.PRESENT, "future memo")
        );
        memberTrainingProfileRepository.save(createTrainingProfile(futureMember.getId()));
        recoveryEmailVerificationTokenRepository.save(createToken(futureMember.getId(), "future@example.com"));
        AttendanceAuditLog futureLog = attendanceAuditLogRepository.save(createAuditLog(futureMember.getId(), futureRecord.getId()));

        int purgedCount = accountDeletionPurgeService.purgeDueMembers();

        Assertions.assertThat(purgedCount).isEqualTo(1);
        Assertions.assertThat(memberRepository.findById(dueMember.getId())).isEmpty();
        Assertions.assertThat(snippetRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(snippetTagRepository.countByIdMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(tagRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(attendanceRecordRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(memberTrainingProfileRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(recoveryEmailVerificationTokenRepository.countByMemberId(dueMember.getId())).isZero();

        AttendanceAuditLog anonymizedLog = attendanceAuditLogRepository.findById(dueLog.getId()).orElseThrow();
        Assertions.assertThat(anonymizedLog.getAction()).isEqualTo(AttendanceAuditAction.DELETE);
        Assertions.assertThat(anonymizedLog.getMemberId()).isNull();
        Assertions.assertThat(anonymizedLog.getAttendanceRecordId()).isNull();
        Assertions.assertThat(anonymizedLog.getChangedByMemberId()).isNull();
        Assertions.assertThat(anonymizedLog.getRequestIpHmac()).isNull();
        Assertions.assertThat(anonymizedLog.getBeforeStatus()).isEqualTo(AttendanceStatus.LATE);
        Assertions.assertThat(anonymizedLog.getAfterStatus()).isEqualTo(AttendanceStatus.ABSENT);

        Assertions.assertThat(memberRepository.findById(futureMember.getId())).isPresent();
        Assertions.assertThat(snippetRepository.countByMemberId(futureMember.getId())).isEqualTo(1);
        Assertions.assertThat(snippetTagRepository.countByIdMemberId(futureMember.getId())).isEqualTo(1);
        Assertions.assertThat(tagRepository.countByMemberId(futureMember.getId())).isEqualTo(1);
        Assertions.assertThat(attendanceRecordRepository.countByMemberId(futureMember.getId())).isEqualTo(1);
        Assertions.assertThat(memberTrainingProfileRepository.countByMemberId(futureMember.getId())).isEqualTo(1);
        Assertions.assertThat(recoveryEmailVerificationTokenRepository.countByMemberId(futureMember.getId())).isEqualTo(1);
        AttendanceAuditLog untouchedFutureLog = attendanceAuditLogRepository.findById(futureLog.getId()).orElseThrow();
        Assertions.assertThat(untouchedFutureLog.getMemberId()).isEqualTo(futureMember.getId());
        Assertions.assertThat(untouchedFutureLog.getAttendanceRecordId()).isEqualTo(futureRecord.getId());
    }

    @Test
    void purgeDueMembersIsIdempotentWhenSomeRelatedDataWasAlreadyRemoved() {
        Member dueMember = createPendingDeleteMember("purge_retry", LocalDateTime.now(clock).minusHours(2));

        Tag dueTag = tagRepository.save(createTag(dueMember.getId(), "retry-tag"));
        Snippet dueSnippet = snippetRepository.save(createSnippet(dueMember.getId(), "retry snippet", "retry content"));
        snippetTagRepository.save(link(dueMember.getId(), dueSnippet.getId(), dueTag.getId()));
        memberTrainingProfileRepository.save(createTrainingProfile(dueMember.getId()));
        recoveryEmailVerificationTokenRepository.save(createToken(dueMember.getId(), "retry@example.com"));
        attendanceAuditLogRepository.save(createAuditLog(dueMember.getId(), null));

        snippetTagRepository.deleteByIdMemberId(dueMember.getId());
        snippetRepository.deleteByMemberId(dueMember.getId());

        int firstRun = accountDeletionPurgeService.purgeDueMembers();
        int secondRun = accountDeletionPurgeService.purgeDueMembers();

        Assertions.assertThat(firstRun).isEqualTo(1);
        Assertions.assertThat(secondRun).isZero();
        Assertions.assertThat(memberRepository.findById(dueMember.getId())).isEmpty();
        Assertions.assertThat(tagRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(memberTrainingProfileRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(recoveryEmailVerificationTokenRepository.countByMemberId(dueMember.getId())).isZero();
        Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .allSatisfy(log -> {
                Assertions.assertThat(log.getMemberId()).isNull();
                Assertions.assertThat(log.getChangedByMemberId()).isNull();
            });
    }

    private Member createPendingDeleteMember(String username, LocalDateTime deleteDueAt) {
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
        member.setUpdatedAt(now);
        return memberRepository.save(member);
    }

    private Snippet createSnippet(Long memberId, String title, String contentMarkdown) {
        Snippet snippet = new Snippet();
        snippet.setMemberId(memberId);
        snippet.setTitle(title);
        snippet.setContentMarkdown(contentMarkdown);
        snippet.setCreatedAt(LocalDateTime.now(clock));
        snippet.setUpdatedAt(LocalDateTime.now(clock));
        return snippet;
    }

    private Tag createTag(Long memberId, String name) {
        Tag tag = new Tag();
        tag.setMemberId(memberId);
        tag.setName(name);
        tag.setNormalizedName(name);
        return tag;
    }

    private AttendanceRecord createAttendanceRecord(Long memberId, LocalDate attendanceDate, AttendanceStatus status, String memo) {
        AttendanceRecord record = new AttendanceRecord();
        record.setMemberId(memberId);
        record.setAttendanceDate(attendanceDate);
        record.setStatus(status);
        record.setMemo(memo);
        record.setCreatedAt(LocalDateTime.now(clock));
        record.setUpdatedAt(LocalDateTime.now(clock));
        return record;
    }

    private RecoveryEmailVerificationToken createToken(Long memberId, String targetEmail) {
        RecoveryEmailVerificationToken token = new RecoveryEmailVerificationToken();
        token.setMemberId(memberId);
        token.setPurpose(RecoveryEmailVerificationPurpose.SIGNUP_VERIFY);
        token.setTargetEmail(targetEmail);
        token.setTokenHash(memberId + "-token-hash-" + targetEmail.hashCode());
        token.setIssuedAt(LocalDateTime.now(clock).minusMinutes(5));
        token.setExpiresAt(LocalDateTime.now(clock).plusMinutes(25));
        return token;
    }

    private AttendanceAuditLog createAuditLog(Long memberId, Long attendanceRecordId) {
        AttendanceAuditLog log = new AttendanceAuditLog();
        log.setAttendanceRecordId(attendanceRecordId);
        log.setMemberId(memberId);
        log.setAction(AttendanceAuditAction.DELETE);
        log.setBeforeAttendanceDate(LocalDate.now(clock).minusDays(1));
        log.setAfterAttendanceDate(LocalDate.now(clock));
        log.setBeforeStatus(AttendanceStatus.LATE);
        log.setAfterStatus(AttendanceStatus.ABSENT);
        log.setChangedByMemberId(memberId);
        log.setChangedAt(LocalDateTime.now(clock));
        log.setRequestIpHmac("abc123");
        return log;
    }

    private MemberTrainingProfile createTrainingProfile(Long memberId) {
        MemberTrainingProfile profile = new MemberTrainingProfile();
        profile.setMemberId(memberId);
        profile.setCourseLabel("삭제 테스트 과정");
        profile.setCourseStartDate(LocalDate.now(clock).minusDays(30));
        profile.setCourseEndDate(LocalDate.now(clock).plusDays(30));
        profile.setAttendanceThresholdPercent(80);
        profile.setDailyAllowanceAmount(15_800);
        profile.setPayableDayCap(20);
        profile.setTrainingDaysCsv("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        profile.setHolidayDatesCsv("");
        profile.setCreatedAt(LocalDateTime.now(clock));
        profile.setUpdatedAt(LocalDateTime.now(clock));
        return profile;
    }

    private SnippetTag link(Long memberId, Long snippetId, Long tagId) {
        SnippetTag snippetTag = new SnippetTag();
        snippetTag.setId(new SnippetTagId(memberId, snippetId, tagId));
        return snippetTag;
    }
}
