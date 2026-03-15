package com.bootsync.attendance.service;

import com.bootsync.attendance.dto.AllowanceSummaryResponse;
import com.bootsync.attendance.dto.MonthlySummaryResponse;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.common.time.AppProperties;
import com.bootsync.member.dto.TrainingProfileRules;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AllowanceCalculator {

    private final AppProperties appProperties;
    private final Clock clock;

    public AllowanceCalculator(AppProperties appProperties, Clock clock) {
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public MonthlySummaryResponse summarize(
        YearMonth yearMonth,
        int presentCount,
        int lateCount,
        int leaveEarlyCount,
        int absentCount
    ) {
        return summarize(
            yearMonth,
            appProperties.getAllowance().getDailyAllowanceAmount(),
            appProperties.getAllowance().getPayableDayCap(),
            presentCount,
            lateCount,
            leaveEarlyCount,
            absentCount
        );
    }

    public MonthlySummaryResponse summarize(
        YearMonth yearMonth,
        int dailyAllowanceAmount,
        int payableDayCap,
        int presentCount,
        int lateCount,
        int leaveEarlyCount,
        int absentCount
    ) {
        int convertedAbsenceCount = absentCount + (lateCount / 3) + (leaveEarlyCount / 3);
        int recordedTrainingDays = presentCount + lateCount + leaveEarlyCount + absentCount;
        int recognizedAttendanceDays = Math.max(0, recordedTrainingDays - convertedAbsenceCount);
        int payableAttendanceDays = Math.min(recognizedAttendanceDays, payableDayCap);
        int deductionAmount = convertedAbsenceCount * dailyAllowanceAmount;
        int expectedAllowanceAmount = payableAttendanceDays * dailyAllowanceAmount;

        return new MonthlySummaryResponse(
            yearMonth.toString(),
            dailyAllowanceAmount * payableDayCap,
            dailyAllowanceAmount,
            presentCount,
            lateCount,
            leaveEarlyCount,
            absentCount,
            convertedAbsenceCount,
            deductionAmount,
            expectedAllowanceAmount
        );
    }

    public AllowanceSummaryResponse summarizeAllowancePeriod(
        YearMonth yearMonth,
        List<AttendanceRecord> records,
        TrainingProfileRules rules
    ) {
        AllowancePeriod period = allowancePeriodFor(yearMonth, rules.courseStartDate(), rules.courseEndDate());
        Set<LocalDate> holidays = rules.holidayDates().stream().collect(Collectors.toSet());
        LocalDate today = LocalDate.now(clock);
        LocalDate completedThroughDate = min(today, period.endDate());

        List<AttendanceRecord> periodRecords = records.stream()
            .filter(record -> !record.getAttendanceDate().isBefore(period.startDate()))
            .filter(record -> !record.getAttendanceDate().isAfter(period.endDate()))
            .filter(record -> isTrainingDay(record.getAttendanceDate(), rules.trainingDays(), holidays))
            .toList();

        int presentCount = count(periodRecords, AttendanceStatus.PRESENT);
        int lateCount = count(periodRecords, AttendanceStatus.LATE);
        int leaveEarlyCount = count(periodRecords, AttendanceStatus.LEAVE_EARLY);
        int absentCount = count(periodRecords, AttendanceStatus.ABSENT);
        int convertedAbsenceCount = absentCount + (lateCount / 3) + (leaveEarlyCount / 3);
        int recordedTrainingDays = periodRecords.size();
        int recognizedAttendanceDays = Math.max(0, recordedTrainingDays - convertedAbsenceCount);
        int payableAttendanceDays = Math.min(recognizedAttendanceDays, rules.payableDayCap());
        int scheduledTrainingDays = countTrainingDays(period.startDate(), period.endDate(), rules.trainingDays(), holidays);
        int completedScheduledDays = countTrainingDays(period.startDate(), completedThroughDate, rules.trainingDays(), holidays);
        int unrecordedCompletedDays = Math.max(0, completedScheduledDays - recordedTrainingDays);

        return new AllowanceSummaryResponse(
            yearMonth.toString(),
            period.startDate().toString(),
            period.endDate().toString(),
            scheduledTrainingDays,
            completedScheduledDays,
            recordedTrainingDays,
            unrecordedCompletedDays,
            presentCount,
            lateCount,
            leaveEarlyCount,
            absentCount,
            convertedAbsenceCount,
            recognizedAttendanceDays,
            payableAttendanceDays,
            rules.dailyAllowanceAmount(),
            rules.payableDayCap(),
            rules.maximumAllowanceAmount(),
            payableAttendanceDays * rules.dailyAllowanceAmount()
        );
    }

    private AllowancePeriod allowancePeriodFor(YearMonth yearMonth, LocalDate courseStartDate, LocalDate courseEndDate) {
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate anchorDate = yearMonth.equals(currentMonth)
            ? LocalDate.now(clock)
            : yearMonth.atEndOfMonth();
        LocalDate referenceDate = clamp(anchorDate, courseStartDate, courseEndDate);
        LocalDate periodStartDate = courseStartDate;

        while (!referenceDate.isBefore(periodStartDate.plusMonths(1))) {
            periodStartDate = periodStartDate.plusMonths(1);
        }

        LocalDate periodEndDate = min(periodStartDate.plusMonths(1).minusDays(1), courseEndDate);
        return new AllowancePeriod(periodStartDate, periodEndDate);
    }

    private int countTrainingDays(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> trainingDays, Set<LocalDate> holidays) {
        if (startDate.isAfter(endDate)) {
            return 0;
        }

        int count = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isTrainingDay(date, trainingDays, holidays)) {
                count++;
            }
        }
        return count;
    }

    private boolean isTrainingDay(LocalDate date, Set<DayOfWeek> trainingDays, Set<LocalDate> holidays) {
        return trainingDays.contains(date.getDayOfWeek()) && !holidays.contains(date);
    }

    private int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream()
            .filter(record -> record.getStatus() == status)
            .count();
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? second : first;
    }

    private LocalDate clamp(LocalDate value, LocalDate minimum, LocalDate maximum) {
        if (value.isBefore(minimum)) {
            return minimum;
        }
        if (value.isAfter(maximum)) {
            return maximum;
        }
        return value;
    }

    private record AllowancePeriod(LocalDate startDate, LocalDate endDate) {
    }
}
