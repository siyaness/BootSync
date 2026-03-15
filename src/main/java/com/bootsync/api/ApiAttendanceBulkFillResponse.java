package com.bootsync.api;

public record ApiAttendanceBulkFillResponse(
    String startDate,
    String endDate,
    int createdCount
) {
}
