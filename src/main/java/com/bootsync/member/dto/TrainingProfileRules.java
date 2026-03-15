package com.bootsync.member.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record TrainingProfileRules(
    String courseLabel,
    LocalDate courseStartDate,
    LocalDate courseEndDate,
    int attendanceThresholdPercent,
    int dailyAllowanceAmount,
    int payableDayCap,
    Set<DayOfWeek> trainingDays,
    List<LocalDate> holidayDates
) {
    public int maximumAllowanceAmount() {
        return Math.max(0, dailyAllowanceAmount * payableDayCap);
    }
}
