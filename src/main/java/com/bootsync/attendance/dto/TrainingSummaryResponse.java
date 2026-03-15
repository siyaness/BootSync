package com.bootsync.attendance.dto;

import java.util.List;

public record TrainingSummaryResponse(
    String courseLabel,
    String courseStartDate,
    String courseEndDate,
    int thresholdPercent,
    int daysUntilCourseEnd,
    int monthScheduledDays,
    int monthCompletedDays,
    int monthRemainingDays,
    List<String> monthHolidayDates,
    int courseScheduledDays,
    int courseCompletedDays,
    int courseRemainingDays,
    int recordedCompletedDays,
    int presentCount,
    int lateCount,
    int leaveEarlyCount,
    int absentCount,
    int unrecordedCompletedDays,
    int effectiveAbsenceCount,
    int effectivePresentDays,
    int attendanceRatePercent,
    int minimumRequiredPresentDays,
    int remainingAbsenceBudget,
    boolean canReachThreshold,
    boolean belowThreshold
) {
}
