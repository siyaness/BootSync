package com.bootsync.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsync.attendance.dto.TrainingSummaryResponse;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
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

class TrainingSummaryServiceTest {

    @Test
    void summarizeUsesConfiguredCourseCalendarCounts() {
        TrainingSummaryService trainingSummaryService = new TrainingSummaryService(fixedClock("2026-03-15T09:00:00Z"));

        TrainingSummaryResponse summary = trainingSummaryService.summarize(
            YearMonth.of(2026, 3),
            List.of(),
            rules(
                LocalDate.of(2025, 9, 23),
                LocalDate.of(2026, 3, 27),
                List.of(
                    LocalDate.of(2025, 10, 3),
                    LocalDate.of(2025, 10, 6),
                    LocalDate.of(2025, 10, 7),
                    LocalDate.of(2025, 10, 8),
                    LocalDate.of(2025, 10, 9),
                    LocalDate.of(2025, 10, 10),
                    LocalDate.of(2025, 12, 25),
                    LocalDate.of(2025, 12, 26),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2),
                    LocalDate.of(2026, 2, 16),
                    LocalDate.of(2026, 2, 17),
                    LocalDate.of(2026, 2, 18),
                    LocalDate.of(2026, 3, 2)
                )
            )
        );

        assertThat(summary.courseLabel()).isEqualTo("국비 과정");
        assertThat(summary.courseStartDate()).isEqualTo("2025-09-23");
        assertThat(summary.courseEndDate()).isEqualTo("2026-03-27");
        assertThat(summary.monthScheduledDays()).isEqualTo(19);
        assertThat(summary.monthCompletedDays()).isEqualTo(9);
        assertThat(summary.monthRemainingDays()).isEqualTo(10);
        assertThat(summary.courseScheduledDays()).isEqualTo(120);
        assertThat(summary.courseCompletedDays()).isEqualTo(110);
        assertThat(summary.courseRemainingDays()).isEqualTo(10);
        assertThat(summary.monthHolidayDates()).containsExactly("2026-03-02");
    }

    @Test
    void summarizeCalculatesAttendanceRateBudgetAndIgnoresWeekendRecords() {
        TrainingSummaryService trainingSummaryService = new TrainingSummaryService(fixedClock("2026-03-09T09:00:00Z"));

        TrainingSummaryResponse summary = trainingSummaryService.summarize(
            YearMonth.of(2026, 3),
            List.of(
                record(LocalDate.of(2026, 3, 2), AttendanceStatus.PRESENT),
                record(LocalDate.of(2026, 3, 3), AttendanceStatus.LATE),
                record(LocalDate.of(2026, 3, 4), AttendanceStatus.LATE),
                record(LocalDate.of(2026, 3, 5), AttendanceStatus.LATE),
                record(LocalDate.of(2026, 3, 6), AttendanceStatus.PRESENT),
                record(LocalDate.of(2026, 3, 7), AttendanceStatus.PRESENT),
                record(LocalDate.of(2026, 3, 9), AttendanceStatus.PRESENT)
            ),
            rules(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 13), List.of())
        );

        assertThat(summary.monthScheduledDays()).isEqualTo(10);
        assertThat(summary.monthCompletedDays()).isEqualTo(6);
        assertThat(summary.monthRemainingDays()).isEqualTo(4);
        assertThat(summary.recordedCompletedDays()).isEqualTo(6);
        assertThat(summary.presentCount()).isEqualTo(3);
        assertThat(summary.lateCount()).isEqualTo(3);
        assertThat(summary.leaveEarlyCount()).isZero();
        assertThat(summary.absentCount()).isZero();
        assertThat(summary.unrecordedCompletedDays()).isZero();
        assertThat(summary.effectiveAbsenceCount()).isEqualTo(1);
        assertThat(summary.effectivePresentDays()).isEqualTo(5);
        assertThat(summary.attendanceRatePercent()).isEqualTo(83);
        assertThat(summary.remainingAbsenceBudget()).isEqualTo(1);
        assertThat(summary.canReachThreshold()).isTrue();
        assertThat(summary.belowThreshold()).isFalse();
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"));
    }

    private TrainingProfileRules rules(LocalDate startDate, LocalDate endDate, List<LocalDate> holidayDates) {
        return new TrainingProfileRules(
            "국비 과정",
            startDate,
            endDate,
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
            holidayDates
        );
    }

    private AttendanceRecord record(LocalDate date, AttendanceStatus status) {
        AttendanceRecord attendanceRecord = new AttendanceRecord();
        attendanceRecord.setAttendanceDate(date);
        attendanceRecord.setStatus(status);
        return attendanceRecord;
    }
}
