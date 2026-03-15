package com.bootsync.settings.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainingProfileRequest(
    String courseLabel,
    LocalDate courseStartDate,
    LocalDate courseEndDate,
    Integer attendanceThresholdPercent,
    Integer dailyAllowanceAmount,
    Integer payableDayCap,
    List<String> trainingDays,
    List<LocalDate> holidayDates
) {
}
