package com.bootsync.attendance.dto;

public record AllowanceSummaryResponse(
    String referenceYearMonth,
    String periodStartDate,
    String periodEndDate,
    int scheduledTrainingDays,
    int completedScheduledDays,
    int recordedTrainingDays,
    int unrecordedCompletedDays,
    int presentCount,
    int lateCount,
    int leaveEarlyCount,
    int absentCount,
    int convertedAbsenceCount,
    int recognizedAttendanceDays,
    int payableAttendanceDays,
    int dailyAllowanceAmount,
    int payableDayCap,
    int maximumAllowanceAmount,
    int expectedAllowanceAmount
) {
}
