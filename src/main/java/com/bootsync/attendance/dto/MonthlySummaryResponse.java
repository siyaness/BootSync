package com.bootsync.attendance.dto;

import java.time.YearMonth;

public record MonthlySummaryResponse(
    String yearMonth,
    int baseAllowanceAmount,
    int absenceDeductionAmount,
    int presentCount,
    int lateCount,
    int leaveEarlyCount,
    int absentCount,
    int convertedAbsenceCount,
    int deductionAmount,
    int expectedAllowanceAmount
) {

    public static MonthlySummaryResponse empty(YearMonth yearMonth, int baseAmount) {
        return new MonthlySummaryResponse(
            yearMonth.toString(),
            baseAmount,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            baseAmount
        );
    }
}
