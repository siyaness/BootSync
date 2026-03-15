package com.bootsync.api;

import com.bootsync.attendance.dto.AllowanceSummaryResponse;
import com.bootsync.attendance.dto.MonthlySummaryResponse;
import com.bootsync.attendance.dto.TrainingSummaryResponse;
import java.util.List;

public record ApiAttendanceMonthResponse(
    String yearMonth,
    MonthlySummaryResponse monthlySummary,
    AllowanceSummaryResponse allowanceSummary,
    TrainingSummaryResponse trainingSummary,
    List<ApiAttendanceRecordResponse> records
) {
}
