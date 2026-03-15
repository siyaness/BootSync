package com.bootsync.attendance.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.bootsync.attendance.dto.AttendanceMonthView;
import com.bootsync.attendance.dto.AttendanceRecordView;
import com.bootsync.attendance.dto.AttendanceUpsertRequest;
import com.bootsync.attendance.dto.AttendanceBulkFillResult;
import com.bootsync.attendance.dto.AllowanceSummaryResponse;
import com.bootsync.attendance.dto.CalendarDayResponse;
import com.bootsync.attendance.dto.MonthlySummaryResponse;
import com.bootsync.attendance.dto.TrainingSummaryResponse;
import com.bootsync.attendance.entity.AttendanceAuditAction;
import com.bootsync.attendance.entity.AttendanceAuditLog;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.config.RequestIpHmacService;
import com.bootsync.member.dto.TrainingProfileRules;
import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.service.MemberTrainingProfileService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendanceService {

    private static final DateTimeFormatter CALENDAR_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM.dd (E)", Locale.KOREAN);

    private final Clock clock;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final MemberRepository memberRepository;
    private final AllowanceCalculator allowanceCalculator;
    private final MemberTrainingProfileService memberTrainingProfileService;
    private final TrainingSummaryService trainingSummaryService;
    private final RequestIpHmacService requestIpHmacService;

    public AttendanceService(
        Clock clock,
        AttendanceRecordRepository attendanceRecordRepository,
        AttendanceAuditLogRepository attendanceAuditLogRepository,
        MemberRepository memberRepository,
        AllowanceCalculator allowanceCalculator,
        MemberTrainingProfileService memberTrainingProfileService,
        TrainingSummaryService trainingSummaryService,
        RequestIpHmacService requestIpHmacService
    ) {
        this.clock = clock;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceAuditLogRepository = attendanceAuditLogRepository;
        this.memberRepository = memberRepository;
        this.allowanceCalculator = allowanceCalculator;
        this.memberTrainingProfileService = memberTrainingProfileService;
        this.trainingSummaryService = trainingSummaryService;
        this.requestIpHmacService = requestIpHmacService;
    }

    public AttendanceMonthView currentMonthViewFor(String username) {
        return monthViewFor(username, YearMonth.now(clock));
    }

    public List<AttendanceRecordView> recordsForMonth(String username, YearMonth yearMonth) {
        return loadMonthRecords(memberIdFor(username), yearMonth).stream()
            .map(record -> new AttendanceRecordView(
                record.getId(),
                record.getAttendanceDate(),
                displayStatus(record.getStatus()),
                record.getMemo()
            ))
            .toList();
    }

    public MonthlySummaryResponse monthlySummaryFor(String username, YearMonth yearMonth) {
        Long memberId = memberIdFor(username);
        return summarize(
            yearMonth,
            loadMonthRecords(memberId, yearMonth),
            memberTrainingProfileService.configuredRulesForMemberId(memberId).orElseGet(memberTrainingProfileService::defaultRules)
        );
    }

    public TrainingSummaryResponse trainingSummaryFor(String username, YearMonth yearMonth) {
        Long memberId = memberIdFor(username);
        return memberTrainingProfileService.configuredRulesForMemberId(memberId)
            .map(trainingProfileRules -> trainingSummaryService.summarize(
                yearMonth,
                attendanceRecordRepository.findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                    memberId,
                    trainingProfileRules.courseStartDate(),
                    trainingProfileRules.courseEndDate()
                ),
                trainingProfileRules
            ))
            .orElse(null);
    }

    public AllowanceSummaryResponse allowanceSummaryFor(String username, YearMonth yearMonth) {
        Long memberId = memberIdFor(username);
        TrainingProfileRules rules = memberTrainingProfileService.configuredRulesForMemberId(memberId)
            .orElseGet(memberTrainingProfileService::defaultRules);
        return allowanceCalculator.summarizeAllowancePeriod(
            yearMonth,
            attendanceRecordRepository.findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                memberId,
                rules.courseStartDate(),
                rules.courseEndDate()
            ),
            rules
        );
    }

    public AttendanceMonthView monthViewFor(String username, YearMonth yearMonth) {
        LocalDate today = LocalDate.now(clock);
        Long memberId = memberIdFor(username);
        List<AttendanceRecord> records = loadMonthRecords(memberId, yearMonth);
        MonthlySummaryResponse monthlySummary = summarize(
            yearMonth,
            records,
            memberTrainingProfileService.configuredRulesForMemberId(memberId).orElseGet(memberTrainingProfileService::defaultRules)
        );

        return new AttendanceMonthView(
            monthlySummary,
            records.stream()
                .map(record -> new CalendarDayResponse(
                    record.getId(),
                    record.getAttendanceDate().format(CALENDAR_DAY_FORMATTER),
                    displayStatus(record.getStatus()),
                    record.getMemo(),
                    record.getAttendanceDate().isEqual(today)
                ))
                .toList(),
            records.stream()
                .filter(record -> record.getAttendanceDate().isEqual(today))
                .findFirst()
                .map(record -> displayStatus(record.getStatus()))
                .orElse("미입력"),
            records.isEmpty()
        );
    }

    public AttendanceUpsertRequest defaultRequestForMonth(String username, YearMonth yearMonth) {
        YearMonth currentMonth = YearMonth.now(clock);
        Long memberId = memberIdFor(username);
        LocalDate defaultDate = yearMonth.equals(currentMonth)
            ? LocalDate.now(clock)
            : yearMonth.atEndOfMonth();

        return attendanceRecordRepository.findByMemberIdAndAttendanceDate(memberId, defaultDate)
            .map(record -> new AttendanceUpsertRequest(
                record.getAttendanceDate(),
                record.getStatus(),
                record.getMemo()
            ))
            .orElseGet(() -> new AttendanceUpsertRequest(defaultDate, AttendanceStatus.PRESENT, ""));
    }

    public AttendanceUpsertRequest requestForRecord(String username, Long attendanceRecordId) {
        AttendanceRecord record = findOwnedRecord(username, attendanceRecordId);
        return new AttendanceUpsertRequest(
            record.getAttendanceDate(),
            record.getStatus(),
            record.getMemo()
        );
    }

    public AttendanceRecordView recordForId(String username, Long attendanceRecordId) {
        AttendanceRecord record = findOwnedRecord(username, attendanceRecordId);
        return new AttendanceRecordView(
            record.getId(),
            record.getAttendanceDate(),
            displayStatus(record.getStatus()),
            record.getMemo()
        );
    }

    public AttendanceBulkFillResult previewBulkFillPresentForConfiguredTrainingDays(String username) {
        return buildBulkFillPlan(memberIdFor(username)).toResult();
    }

    @Transactional
    public AttendanceBulkFillResult bulkFillPresentForConfiguredTrainingDays(String username, String clientIp) {
        Long memberId = memberIdFor(username);
        BulkFillPlan bulkFillPlan = buildBulkFillPlan(memberId);
        LocalDateTime now = LocalDateTime.now(clock);
        String requestIpHmac = requestIpHmacService.hmac(clientIp);
        List<AttendanceRecord> recordsToCreate = new ArrayList<>();

        for (LocalDate date : bulkFillPlan.datesToCreate()) {
            AttendanceRecord record = new AttendanceRecord();
            record.setMemberId(memberId);
            record.setAttendanceDate(date);
            record.setStatus(AttendanceStatus.PRESENT);
            record.setMemo(null);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            recordsToCreate.add(record);
        }

        if (recordsToCreate.isEmpty()) {
            return bulkFillPlan.toResult();
        }

        List<AttendanceRecord> savedRecords = attendanceRecordRepository.saveAll(recordsToCreate);
        List<AttendanceAuditLog> auditLogs = savedRecords.stream()
            .map(record -> buildAuditLog(
                record.getId(),
                memberId,
                AttendanceAuditAction.CREATE,
                null,
                record.getAttendanceDate(),
                null,
                record.getStatus(),
                now,
                requestIpHmac
            ))
            .toList();
        attendanceAuditLogRepository.saveAll(auditLogs);

        return new AttendanceBulkFillResult(
            bulkFillPlan.startDate().toString(),
            bulkFillPlan.endDate().toString(),
            savedRecords.size()
        );
    }

    @Transactional
    public boolean upsertFor(String username, AttendanceUpsertRequest request, String clientIp) {
        Long memberId = memberIdFor(username);
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedMemo = StringUtils.hasText(request.memo()) ? request.memo().trim() : null;
        String requestIpHmac = requestIpHmacService.hmac(clientIp);
        validateAttendanceDate(request.attendanceDate());

        AttendanceRecord existingRecord = attendanceRecordRepository.findByMemberIdAndAttendanceDate(memberId, request.attendanceDate())
            .orElse(null);

        if (existingRecord == null) {
            AttendanceRecord record = new AttendanceRecord();
            record.setMemberId(memberId);
            record.setAttendanceDate(request.attendanceDate());
            record.setStatus(request.status());
            record.setMemo(normalizedMemo);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);

            AttendanceRecord savedRecord = attendanceRecordRepository.save(record);
            attendanceAuditLogRepository.save(buildAuditLog(
                savedRecord.getId(),
                memberId,
                AttendanceAuditAction.CREATE,
                null,
                savedRecord.getAttendanceDate(),
                null,
                savedRecord.getStatus(),
                now,
                requestIpHmac
            ));
            return false;
        }

        AttendanceStatus beforeStatus = existingRecord.getStatus();
        LocalDate beforeDate = existingRecord.getAttendanceDate();
        existingRecord.setStatus(request.status());
        existingRecord.setMemo(normalizedMemo);
        existingRecord.setUpdatedAt(now);
        AttendanceRecord savedRecord = attendanceRecordRepository.save(existingRecord);

        attendanceAuditLogRepository.save(buildAuditLog(
            savedRecord.getId(),
            memberId,
            AttendanceAuditAction.UPDATE,
            beforeDate,
            savedRecord.getAttendanceDate(),
            beforeStatus,
            savedRecord.getStatus(),
            now,
            requestIpHmac
        ));
        return true;
    }

    @Transactional
    public LocalDate updateByIdFor(String username, Long attendanceRecordId, AttendanceUpsertRequest request, String clientIp) {
        Long memberId = memberIdFor(username);
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedMemo = StringUtils.hasText(request.memo()) ? request.memo().trim() : null;
        String requestIpHmac = requestIpHmacService.hmac(clientIp);
        validateAttendanceDate(request.attendanceDate());
        AttendanceRecord record = findOwnedRecord(memberId, attendanceRecordId);

        attendanceRecordRepository.findByMemberIdAndAttendanceDate(memberId, request.attendanceDate())
            .filter(existingRecord -> !existingRecord.getId().equals(attendanceRecordId))
            .ifPresent(existingRecord -> {
                throw new AttendanceRequestValidationException("attendanceDate", "같은 날짜 출결이 이미 있습니다.");
            });

        LocalDate beforeDate = record.getAttendanceDate();
        AttendanceStatus beforeStatus = record.getStatus();

        record.setAttendanceDate(request.attendanceDate());
        record.setStatus(request.status());
        record.setMemo(normalizedMemo);
        record.setUpdatedAt(now);
        AttendanceRecord savedRecord = attendanceRecordRepository.save(record);

        attendanceAuditLogRepository.save(buildAuditLog(
            savedRecord.getId(),
            memberId,
            AttendanceAuditAction.UPDATE,
            beforeDate,
            savedRecord.getAttendanceDate(),
            beforeStatus,
            savedRecord.getStatus(),
            now,
            requestIpHmac
        ));

        return savedRecord.getAttendanceDate();
    }

    @Transactional
    public LocalDate deleteFor(String username, Long attendanceRecordId, String clientIp) {
        Long memberId = memberIdFor(username);
        AttendanceRecord record = findOwnedRecord(memberId, attendanceRecordId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate deletedDate = record.getAttendanceDate();
        AttendanceStatus deletedStatus = record.getStatus();
        String requestIpHmac = requestIpHmacService.hmac(clientIp);

        attendanceRecordRepository.delete(record);
        attendanceAuditLogRepository.save(buildAuditLog(
            record.getId(),
            memberId,
            AttendanceAuditAction.DELETE,
            deletedDate,
            null,
            deletedStatus,
            null,
            now,
            requestIpHmac
        ));

        return deletedDate;
    }

    private List<AttendanceRecord> loadMonthRecords(Long memberId, YearMonth yearMonth) {
        return attendanceRecordRepository.findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            memberId,
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth()
        );
    }

    private Long memberIdFor(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return member.getId();
    }

    private AttendanceRecord findOwnedRecord(String username, Long attendanceRecordId) {
        return findOwnedRecord(memberIdFor(username), attendanceRecordId);
    }

    private AttendanceRecord findOwnedRecord(Long memberId, Long attendanceRecordId) {
        return attendanceRecordRepository.findByIdAndMemberId(attendanceRecordId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private void validateAttendanceDate(LocalDate attendanceDate) {
        if (attendanceDate.isAfter(LocalDate.now(clock))) {
            throw new AttendanceRequestValidationException("attendanceDate", "미래 날짜 출결은 저장할 수 없습니다.");
        }
    }

    private MonthlySummaryResponse summarize(YearMonth yearMonth, List<AttendanceRecord> records, TrainingProfileRules trainingProfileRules) {
        int presentCount = count(records, AttendanceStatus.PRESENT);
        int lateCount = count(records, AttendanceStatus.LATE);
        int leaveEarlyCount = count(records, AttendanceStatus.LEAVE_EARLY);
        int absentCount = count(records, AttendanceStatus.ABSENT);

        return allowanceCalculator.summarize(
            yearMonth,
            trainingProfileRules.dailyAllowanceAmount(),
            trainingProfileRules.payableDayCap(),
            presentCount,
            lateCount,
            leaveEarlyCount,
            absentCount
        );
    }

    private boolean isConfiguredTrainingDay(LocalDate date, TrainingProfileRules rules) {
        if (date.isBefore(rules.courseStartDate()) || date.isAfter(rules.courseEndDate())) {
            return false;
        }
        if (rules.holidayDates().contains(date)) {
            return false;
        }
        return rules.trainingDays().contains(date.getDayOfWeek());
    }

    private BulkFillPlan buildBulkFillPlan(Long memberId) {
        TrainingProfileRules rules = memberTrainingProfileService.configuredRulesForMemberId(memberId)
            .orElseThrow(() -> new AttendanceRequestValidationException(
                "trainingProfile",
                "과정 현황에서 먼저 내 과정 정보를 저장해 주세요."
            ));

        LocalDate startDate = rules.courseStartDate();
        LocalDate endDate = LocalDate.now(clock);
        if (endDate.isAfter(rules.courseEndDate())) {
            endDate = rules.courseEndDate();
        }

        if (endDate.isBefore(startDate)) {
            return new BulkFillPlan(startDate, endDate, List.of());
        }

        List<AttendanceRecord> existingRecords = attendanceRecordRepository
            .findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(memberId, startDate, endDate);
        Set<LocalDate> existingDates = existingRecords.stream()
            .map(AttendanceRecord::getAttendanceDate)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        List<LocalDate> datesToCreate = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (existingDates.contains(date) || !isConfiguredTrainingDay(date, rules)) {
                continue;
            }
            datesToCreate.add(date);
        }

        return new BulkFillPlan(startDate, endDate, datesToCreate);
    }

    private int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream()
            .filter(record -> record.getStatus() == status)
            .count();
    }

    private String displayStatus(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "출석";
            case LATE -> "지각";
            case LEAVE_EARLY -> "조퇴";
            case ABSENT -> "결석";
        };
    }

    private AttendanceAuditLog buildAuditLog(
        Long attendanceRecordId,
        Long memberId,
        AttendanceAuditAction action,
        LocalDate beforeAttendanceDate,
        LocalDate afterAttendanceDate,
        AttendanceStatus beforeStatus,
        AttendanceStatus afterStatus,
        LocalDateTime changedAt,
        String requestIpHmac
    ) {
        AttendanceAuditLog log = new AttendanceAuditLog();
        log.setAttendanceRecordId(attendanceRecordId);
        log.setMemberId(memberId);
        log.setAction(action);
        log.setBeforeAttendanceDate(beforeAttendanceDate);
        log.setAfterAttendanceDate(afterAttendanceDate);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setChangedByMemberId(memberId);
        log.setChangedAt(changedAt);
        log.setRequestIpHmac(requestIpHmac);
        return log;
    }

    private record BulkFillPlan(
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> datesToCreate
    ) {
        private AttendanceBulkFillResult toResult() {
            return new AttendanceBulkFillResult(startDate.toString(), endDate.toString(), datesToCreate.size());
        }
    }
}
