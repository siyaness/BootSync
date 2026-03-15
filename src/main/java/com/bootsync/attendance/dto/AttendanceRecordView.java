package com.bootsync.attendance.dto;

import java.time.LocalDate;

public record AttendanceRecordView(
    Long id,
    LocalDate attendanceDate,
    String status,
    String memo
) {
}
