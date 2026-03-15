package com.bootsync.api;

import com.bootsync.attendance.dto.AttendanceRecordView;
import com.bootsync.attendance.dto.AttendanceUpsertRequest;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.service.AttendanceRequestValidationException;
import com.bootsync.attendance.service.AttendanceService;
import com.bootsync.config.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class ApiAttendanceController {

    private final Clock clock;
    private final AttendanceService attendanceService;
    private final ClientIpResolver clientIpResolver;

    public ApiAttendanceController(Clock clock, AttendanceService attendanceService, ClientIpResolver clientIpResolver) {
        this.clock = clock;
        this.attendanceService = attendanceService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public ApiAttendanceMonthResponse month(
        Authentication authentication,
        @RequestParam(name = "yearMonth", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        YearMonth selectedYearMonth = normalizeYearMonth(yearMonth);
        return monthResponse(authentication.getName(), selectedYearMonth);
    }

    @GetMapping("/record/{attendanceRecordId}")
    public ApiAttendanceRecordResponse record(
        Authentication authentication,
        @PathVariable Long attendanceRecordId
    ) {
        return toResponse(attendanceService.recordForId(authentication.getName(), attendanceRecordId));
    }

    @GetMapping("/bulk-fill/present/preview")
    public com.bootsync.attendance.dto.AttendanceBulkFillResult bulkFillPresentPreview(Authentication authentication) {
        return attendanceService.previewBulkFillPresentForConfiguredTrainingDays(authentication.getName());
    }

    @PostMapping("/bulk-fill/present")
    public com.bootsync.attendance.dto.AttendanceBulkFillResult bulkFillPresent(
        Authentication authentication,
        HttpServletRequest httpServletRequest
    ) {
        return attendanceService.bulkFillPresentForConfiguredTrainingDays(
            authentication.getName(),
            clientIpResolver.resolve(httpServletRequest)
        );
    }

    @PutMapping("/{attendanceDate}")
    public ApiAttendanceMonthResponse upsert(
        Authentication authentication,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
        @Valid @RequestBody ApiAttendanceSaveRequest request,
        HttpServletRequest httpServletRequest
    ) {
        AttendanceUpsertRequest upsertRequest = new AttendanceUpsertRequest(
            attendanceDate,
            parseStatus(request.status()),
            request.memo()
        );
        attendanceService.upsertFor(authentication.getName(), upsertRequest, clientIpResolver.resolve(httpServletRequest));
        return monthResponse(authentication.getName(), YearMonth.from(attendanceDate));
    }

    @DeleteMapping("/{attendanceRecordId}")
    public ApiAttendanceMonthResponse delete(
        Authentication authentication,
        @PathVariable Long attendanceRecordId,
        HttpServletRequest httpServletRequest
    ) {
        LocalDate deletedDate = attendanceService.deleteFor(
            authentication.getName(),
            attendanceRecordId,
            clientIpResolver.resolve(httpServletRequest)
        );
        return monthResponse(authentication.getName(), YearMonth.from(deletedDate));
    }

    private ApiAttendanceMonthResponse monthResponse(String username, YearMonth yearMonth) {
        List<ApiAttendanceRecordResponse> records = attendanceService.recordsForMonth(username, yearMonth).stream()
            .map(this::toResponse)
            .toList();
        return new ApiAttendanceMonthResponse(
            yearMonth.toString(),
            attendanceService.monthlySummaryFor(username, yearMonth),
            attendanceService.allowanceSummaryFor(username, yearMonth),
            attendanceService.trainingSummaryFor(username, yearMonth),
            records
        );
    }

    private ApiAttendanceRecordResponse toResponse(AttendanceRecordView record) {
        return new ApiAttendanceRecordResponse(
            record.id(),
            record.attendanceDate().toString(),
            record.status(),
            record.memo()
        );
    }

    private YearMonth normalizeYearMonth(YearMonth yearMonth) {
        YearMonth currentMonth = YearMonth.now(clock);
        if (yearMonth == null || yearMonth.isAfter(currentMonth)) {
            return currentMonth;
        }
        return yearMonth;
    }

    private AttendanceStatus parseStatus(String status) {
        return switch (status) {
            case "출석" -> AttendanceStatus.PRESENT;
            case "지각" -> AttendanceStatus.LATE;
            case "조퇴" -> AttendanceStatus.LEAVE_EARLY;
            case "결석" -> AttendanceStatus.ABSENT;
            default -> throw new AttendanceRequestValidationException("status", "유효하지 않은 출결 상태입니다.");
        };
    }
}
