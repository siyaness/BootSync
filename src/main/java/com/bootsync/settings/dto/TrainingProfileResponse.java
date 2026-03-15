package com.bootsync.settings.dto;

import java.util.List;

public record TrainingProfileResponse(
    boolean configured,
    String courseLabel,
    String courseStartDate,
    String courseEndDate,
    int attendanceThresholdPercent,
    int dailyAllowanceAmount,
    int payableDayCap,
    int maximumAllowanceAmount,
    List<String> trainingDays,
    List<String> holidayDates
) {
}
