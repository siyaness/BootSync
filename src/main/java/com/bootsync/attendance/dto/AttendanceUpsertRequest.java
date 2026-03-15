package com.bootsync.attendance.dto;

import com.bootsync.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AttendanceUpsertRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull @PastOrPresent LocalDate attendanceDate,
    @NotNull AttendanceStatus status,
    @Size(max = 255) String memo
) {
}
