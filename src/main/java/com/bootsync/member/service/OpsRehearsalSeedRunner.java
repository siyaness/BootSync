package com.bootsync.member.service;

import com.bootsync.attendance.entity.AttendanceAuditAction;
import com.bootsync.attendance.entity.AttendanceAuditLog;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import com.bootsync.member.entity.RecoveryEmailVerificationToken;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.entity.Tag;
import com.bootsync.tag.repository.TagRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
@Order(25)
@ConditionalOnProperty(name = "app.operations.rehearsal-seed.enabled", havingValue = "true")
public class OpsRehearsalSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpsRehearsalSeedRunner.class);

    static final String RESET_USERNAME = "ops_reset_target";
    static final String PURGE_USERNAME = "ops_purge_due_target";
    static final String SCRUB_USERNAME = "ops_scrub_target";

    private final ConfigurableApplicationContext applicationContext;
    private final MemberRepository memberRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final SnippetRepository snippetRepository;
    private final SnippetTagRepository snippetTagRepository;
    private final TagRepository tagRepository;
    private final RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final boolean closeContextAfterRun;

    public OpsRehearsalSeedRunner(
        ConfigurableApplicationContext applicationContext,
        MemberRepository memberRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        AttendanceAuditLogRepository attendanceAuditLogRepository,
        SnippetRepository snippetRepository,
        SnippetTagRepository snippetTagRepository,
        TagRepository tagRepository,
        RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository,
        PasswordEncoder passwordEncoder,
        Clock clock,
        @Value("${app.operations.rehearsal-seed.close-context-after-run:true}") boolean closeContextAfterRun
    ) {
        this.applicationContext = applicationContext;
        this.memberRepository = memberRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceAuditLogRepository = attendanceAuditLogRepository;
        this.snippetRepository = snippetRepository;
        this.snippetTagRepository = snippetTagRepository;
        this.tagRepository = tagRepository;
        this.recoveryEmailVerificationTokenRepository = recoveryEmailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.closeContextAfterRun = closeContextAfterRun;
    }

    @Override
    public void run(ApplicationArguments args) {
        Member resetTarget = upsertResetTarget();
        Member purgeTarget = upsertDeletionTarget();
        Member scrubTarget = upsertScrubTarget();

        seedExerciseData(purgeTarget, "purge");
        seedExerciseData(scrubTarget, "scrub");

        log.info(
            "BootSync ops rehearsal seed completed: resetUsername={}, resetMemberId={}, purgeUsername={}, purgeMemberId={}, scrubUsername={}, scrubMemberId={}",
            resetTarget.getUsername(),
            resetTarget.getId(),
            purgeTarget.getUsername(),
            purgeTarget.getId(),
            scrubTarget.getUsername(),
            scrubTarget.getId()
        );

        if (closeContextAfterRun) {
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }

    private Member upsertResetTarget() {
        LocalDateTime now = LocalDateTime.now(clock);
        Member member = memberRepository.findByUsername(RESET_USERNAME).orElseGet(Member::new);
        member.setUsername(RESET_USERNAME);
        member.setPasswordHash(passwordEncoder.encode("ops-reset-before-123"));
        member.setDisplayName("Ops Reset Target");
        member.setRecoveryEmail("ops-reset-target@example.com");
        member.setRecoveryEmailVerifiedAt(now.minusHours(1));
        member.setStatus(MemberStatus.ACTIVE);
        member.setDeleteRequestedAt(null);
        member.setDeleteDueAt(null);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now.minusDays(2));
        }
        member.setUpdatedAt(now.minusMinutes(5));
        return memberRepository.save(member);
    }

    private Member upsertDeletionTarget() {
        LocalDateTime now = LocalDateTime.now(clock);
        Member member = memberRepository.findByUsername(PURGE_USERNAME).orElseGet(Member::new);
        member.setUsername(PURGE_USERNAME);
        member.setPasswordHash(passwordEncoder.encode("ops-purge-before-123"));
        member.setDisplayName("Ops Purge Target");
        member.setRecoveryEmail("ops-purge-target@example.com");
        member.setRecoveryEmailVerifiedAt(now.minusDays(5));
        member.setStatus(MemberStatus.PENDING_DELETE);
        member.setDeleteRequestedAt(now.minusDays(8));
        member.setDeleteDueAt(now.minusHours(2));
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now.minusDays(10));
        }
        member.setUpdatedAt(now.minusMinutes(10));
        return memberRepository.save(member);
    }

    private Member upsertScrubTarget() {
        LocalDateTime now = LocalDateTime.now(clock);
        Member member = memberRepository.findByUsername(SCRUB_USERNAME).orElseGet(Member::new);
        member.setUsername(SCRUB_USERNAME);
        member.setPasswordHash(passwordEncoder.encode("ops-scrub-before-123"));
        member.setDisplayName("Ops Scrub Target");
        member.setRecoveryEmail("ops-scrub-target@example.com");
        member.setRecoveryEmailVerifiedAt(now.minusDays(4));
        member.setStatus(MemberStatus.ACTIVE);
        member.setDeleteRequestedAt(null);
        member.setDeleteDueAt(null);
        if (member.getCreatedAt() == null) {
            member.setCreatedAt(now.minusDays(7));
        }
        member.setUpdatedAt(now.minusMinutes(7));
        return memberRepository.save(member);
    }

    private void seedExerciseData(Member member, String key) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (tagRepository.countByMemberId(member.getId()) == 0L) {
            Tag tag = new Tag();
            tag.setMemberId(member.getId());
            tag.setName("ops-" + key);
            tag.setNormalizedName("ops-" + key);
            Tag savedTag = tagRepository.save(tag);

            Snippet snippet = new Snippet();
            snippet.setMemberId(member.getId());
            snippet.setTitle("ops-" + key + "-snippet");
            snippet.setContentMarkdown("ops " + key + " rehearsal data");
            snippet.setCreatedAt(now.minusDays(2));
            snippet.setUpdatedAt(now.minusHours(3));
            Snippet savedSnippet = snippetRepository.save(snippet);

            SnippetTag snippetTag = new SnippetTag();
            snippetTag.setId(new SnippetTagId(member.getId(), savedSnippet.getId(), savedTag.getId()));
            snippetTagRepository.save(snippetTag);
        }

        if (attendanceRecordRepository.countByMemberId(member.getId()) == 0L) {
            AttendanceRecord attendanceRecord = new AttendanceRecord();
            attendanceRecord.setMemberId(member.getId());
            attendanceRecord.setAttendanceDate(LocalDate.now(clock).minusDays(1));
            attendanceRecord.setStatus(AttendanceStatus.ABSENT);
            attendanceRecord.setMemo("ops " + key + " rehearsal record");
            attendanceRecord.setCreatedAt(now.minusDays(1));
            attendanceRecord.setUpdatedAt(now.minusHours(4));
            AttendanceRecord savedRecord = attendanceRecordRepository.save(attendanceRecord);

            if (attendanceAuditLogRepository.countByMemberIdOrChangedByMemberId(member.getId(), member.getId()) == 0L) {
                AttendanceAuditLog auditLog = new AttendanceAuditLog();
                auditLog.setAttendanceRecordId(savedRecord.getId());
                auditLog.setMemberId(member.getId());
                auditLog.setAction(AttendanceAuditAction.CREATE);
                auditLog.setAfterAttendanceDate(savedRecord.getAttendanceDate());
                auditLog.setAfterStatus(savedRecord.getStatus());
                auditLog.setChangedByMemberId(member.getId());
                auditLog.setChangedAt(now.minusHours(2));
                auditLog.setRequestIpHmac(hashValue("audit-" + key));
                attendanceAuditLogRepository.save(auditLog);
            }
        }

        if (recoveryEmailVerificationTokenRepository.countByMemberId(member.getId()) == 0L) {
            RecoveryEmailVerificationToken token = new RecoveryEmailVerificationToken();
            token.setMemberId(member.getId());
            token.setPurpose(RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE);
            token.setTargetEmail("ops-" + key + "-pending@example.com");
            token.setTokenHash(hashValue("token-" + key));
            token.setIssuedAt(now.minusMinutes(30));
            token.setExpiresAt(now.plusMinutes(30));
            recoveryEmailVerificationTokenRepository.save(token);
        }
    }

    private String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }
}
