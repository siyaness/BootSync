package com.bootsync.api;

public record ApiAttendanceRecordResponse(
    Long id,
    String date,
    String status,
    String memo
) {
}
