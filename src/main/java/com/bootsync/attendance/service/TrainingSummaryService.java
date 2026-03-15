package com.bootsync.attendance.service;

import com.bootsync.attendance.dto.TrainingSummaryResponse;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.member.dto.TrainingProfileRules;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TrainingSummaryService {

    private final Clock clock;

    public TrainingSummaryService(Clock clock) {
        this.clock = clock;
    }

    public TrainingSummaryResponse summarize(YearMonth yearMonth, List<AttendanceRecord> records, TrainingProfileRules rules) {
        validateCourseWindow(rules);

        LocalDate courseStartDate = rules.courseStartDate();
        LocalDate courseEndDate = rules.courseEndDate();
        LocalDate today = LocalDate.now(clock);
        LocalDate completedThroughDate = min(today, courseEndDate);
        Set<LocalDate> holidays = rules.holidayDates().stream().collect(Collectors.toSet());

        int courseScheduledDays = countTrainingDays(courseStartDate, courseEndDate, rules.trainingDays(), holidays);
        int courseCompletedDays = countTrainingDays(courseStartDate, completedThroughDate, rules.trainingDays(), holidays);
        int courseRemainingDays = Math.max(0, courseScheduledDays - courseCompletedDays);

        LocalDate monthStartDate = max(yearMonth.atDay(1), courseStartDate);
        LocalDate monthEndDate = min(yearMonth.atEndOfMonth(), courseEndDate);
        int monthScheduledDays = countTrainingDays(monthStartDate, monthEndDate, rules.trainingDays(), holidays);
        int monthCompletedDays = countTrainingDays(monthStartDate, min(monthEndDate, completedThroughDate), rules.trainingDays(), holidays);
        int monthRemainingDays = Math.max(0, monthScheduledDays - monthCompletedDays);

        List<AttendanceRecord> completedTrainingRecords = records.stream()
            .filter(record -> !record.getAttendanceDate().isBefore(courseStartDate))
            .filter(record -> !record.getAttendanceDate().isAfter(completedThroughDate))
            .filter(record -> isTrainingDay(record.getAttendanceDate(), rules.trainingDays(), holidays))
            .toList();

        int recordedCompletedDays = completedTrainingRecords.size();
        int presentCount = count(completedTrainingRecords, AttendanceStatus.PRESENT);
        int absentCount = count(completedTrainingRecords, AttendanceStatus.ABSENT);
        int lateCount = count(completedTrainingRecords, AttendanceStatus.LATE);
        int leaveEarlyCount = count(completedTrainingRecords, AttendanceStatus.LEAVE_EARLY);
        int effectiveAbsenceCount = absentCount + (lateCount / 3) + (leaveEarlyCount / 3);
        int effectivePresentDays = Math.max(0, recordedCompletedDays - effectiveAbsenceCount);
        int unrecordedCompletedDays = Math.max(0, courseCompletedDays - recordedCompletedDays);
        int minimumRequiredPresentDays = (int) Math.ceil(
            courseScheduledDays * (rules.attendanceThresholdPercent() / 100.0)
        );
        int remainingAbsenceBudget = Math.max(
            0,
            effectivePresentDays + courseRemainingDays - minimumRequiredPresentDays
        );
        boolean canReachThreshold = effectivePresentDays + courseRemainingDays >= minimumRequiredPresentDays;
        int attendanceRatePercent = courseCompletedDays == 0
            ? 0
            : (int) Math.round((effectivePresentDays * 100.0) / courseCompletedDays);
        boolean belowThreshold = courseCompletedDays > 0 && attendanceRatePercent < rules.attendanceThresholdPercent();

        List<String> monthHolidayDates = rules.holidayDates().stream()
            .filter(date -> YearMonth.from(date).equals(yearMonth))
            .sorted(Comparator.naturalOrder())
            .map(LocalDate::toString)
            .toList();

        return new TrainingSummaryResponse(
            rules.courseLabel(),
            courseStartDate.toString(),
            courseEndDate.toString(),
            rules.attendanceThresholdPercent(),
            (int) ChronoUnit.DAYS.between(today, courseEndDate),
            monthScheduledDays,
            monthCompletedDays,
            monthRemainingDays,
            monthHolidayDates,
            courseScheduledDays,
            courseCompletedDays,
            courseRemainingDays,
            recordedCompletedDays,
            presentCount,
            lateCount,
            leaveEarlyCount,
            absentCount,
            unrecordedCompletedDays,
            effectiveAbsenceCount,
            effectivePresentDays,
            attendanceRatePercent,
            minimumRequiredPresentDays,
            remainingAbsenceBudget,
            canReachThreshold,
            belowThreshold
        );
    }

    private void validateCourseWindow(TrainingProfileRules rules) {
        if (rules.courseEndDate().isBefore(rules.courseStartDate())) {
            throw new IllegalStateException("training profile course end date must not be before course start date");
        }
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
        return trainingDays.contains(date.getDayOfWeek())
            && !holidays.contains(date);
    }

    private int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream()
            .filter(record -> record.getStatus() == status)
            .count();
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? second : first;
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }
}
