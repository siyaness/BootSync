package com.bootsync.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsync.attendance.dto.AllowanceSummaryResponse;
import com.bootsync.attendance.dto.MonthlySummaryResponse;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.common.time.AppProperties;
import com.bootsync.member.dto.TrainingProfileRules;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllowanceCalculatorTest {

    private final AllowanceCalculator allowanceCalculator = new AllowanceCalculator(
        new AppProperties(),
        Clock.fixed(Instant.parse("2026-03-15T09:00:00Z"), ZoneId.of("Asia/Seoul"))
    );

    @Test
    void lateAndLeaveEarlyAreConvertedSeparately() {
        MonthlySummaryResponse summary = allowanceCalculator.summarize(
            YearMonth.of(2026, 3),
            0,
            2,
            1,
            0
        );

        assertThat(summary.convertedAbsenceCount()).isZero();
        assertThat(summary.deductionAmount()).isZero();
        assertThat(summary.expectedAllowanceAmount()).isEqualTo(47_400);
    }

    @Test
    void lateAndLeaveEarlyOnlyConvertInOwnBuckets() {
        MonthlySummaryResponse summary = allowanceCalculator.summarize(
            YearMonth.of(2026, 3),
            0,
            4,
            5,
            0
        );

        assertThat(summary.convertedAbsenceCount()).isEqualTo(2);
        assertThat(summary.deductionAmount()).isEqualTo(31_600);
        assertThat(summary.expectedAllowanceAmount()).isEqualTo(110_600);
    }

    @Test
    void allowancePeriodSummaryCapsPaidDaysAtTwenty() {
        AllowanceSummaryResponse summary = allowanceCalculator.summarizeAllowancePeriod(
            YearMonth.of(2025, 10),
            List.of(
                record(LocalDate.of(2025, 10, 23), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 24), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 27), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 28), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 29), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 30), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 10, 31), AttendanceStatus.LEAVE_EARLY),
                record(LocalDate.of(2025, 11, 3), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 4), AttendanceStatus.ABSENT),
                record(LocalDate.of(2025, 11, 5), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 6), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 7), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 10), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 11), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 12), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 13), AttendanceStatus.LEAVE_EARLY),
                record(LocalDate.of(2025, 11, 14), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 17), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 18), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 19), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 20), AttendanceStatus.PRESENT),
                record(LocalDate.of(2025, 11, 21), AttendanceStatus.PRESENT)
            ),
            new TrainingProfileRules(
                "국비 과정",
                LocalDate.of(2025, 9, 23),
                LocalDate.of(2026, 3, 27),
                80,
                15_800,
                20,
                EnumSet.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ),
                List.of()
            )
        );

        assertThat(summary.periodStartDate()).isEqualTo("2025-10-23");
        assertThat(summary.periodEndDate()).isEqualTo("2025-11-22");
        assertThat(summary.convertedAbsenceCount()).isEqualTo(1);
        assertThat(summary.recognizedAttendanceDays()).isEqualTo(21);
        assertThat(summary.payableAttendanceDays()).isEqualTo(20);
        assertThat(summary.expectedAllowanceAmount()).isEqualTo(316_000);
    }

    private AttendanceRecord record(LocalDate date, AttendanceStatus status) {
        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setAttendanceDate(date);
        attendanceRecord.setStatus(status);
        return attendanceRecord;
    }
}
