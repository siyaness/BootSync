package com.bootsync.attendance.dto;

public record CalendarDayResponse(
    Long id,
    String date,
    String status,
    String memo,
    boolean today
) {
}
