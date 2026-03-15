package com.bootsync.attendance.dto;

import java.util.List;

public record AttendanceMonthView(
    MonthlySummaryResponse monthlySummary,
    List<CalendarDayResponse> calendarDays,
    String todayStatus,
    boolean empty
) {
}
