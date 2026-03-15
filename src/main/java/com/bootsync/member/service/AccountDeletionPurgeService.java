package com.bootsync.member.service;

import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.MemberTrainingProfileRepository;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.repository.TagRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionPurgeService {

    private final MemberRepository memberRepository;
    private final SnippetTagRepository snippetTagRepository;
    private final SnippetRepository snippetRepository;
    private final TagRepository tagRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final MemberTrainingProfileRepository memberTrainingProfileRepository;
    private final RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;
    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final Clock clock;

    public AccountDeletionPurgeService(
        MemberRepository memberRepository,
        SnippetTagRepository snippetTagRepository,
        SnippetRepository snippetRepository,
        TagRepository tagRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        MemberTrainingProfileRepository memberTrainingProfileRepository,
        RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository,
        AttendanceAuditLogRepository attendanceAuditLogRepository,
        Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.snippetTagRepository = snippetTagRepository;
        this.snippetRepository = snippetRepository;
        this.tagRepository = tagRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.memberTrainingProfileRepository = memberTrainingProfileRepository;
        this.recoveryEmailVerificationTokenRepository = recoveryEmailVerificationTokenRepository;
        this.attendanceAuditLogRepository = attendanceAuditLogRepository;
        this.clock = clock;
    }

    @Transactional
    public int purgeDueMembers() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Member> dueMembers = memberRepository.findByStatusAndDeleteDueAtLessThanEqualOrderByDeleteDueAtAscIdAsc(
            MemberStatus.PENDING_DELETE,
            now
        );

        for (Member dueMember : dueMembers) {
            purgeMemberData(dueMember.getId());
        }

        return dueMembers.size();
    }

    private void purgeMemberData(Long memberId) {
        snippetTagRepository.deleteByIdMemberId(memberId);
        snippetRepository.deleteByMemberId(memberId);
        tagRepository.deleteByMemberId(memberId);
        attendanceRecordRepository.deleteByMemberId(memberId);
        memberTrainingProfileRepository.deleteByMemberId(memberId);
        recoveryEmailVerificationTokenRepository.deleteByMemberId(memberId);
        attendanceAuditLogRepository.anonymizeByMemberId(memberId);
        memberRepository.deleteById(memberId);
    }
}
