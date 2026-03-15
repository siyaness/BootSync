package com.bootsync.attendance.dto;

public record AttendanceBulkFillResult(
    String startDate,
    String endDate,
    int createdCount
) {
}
