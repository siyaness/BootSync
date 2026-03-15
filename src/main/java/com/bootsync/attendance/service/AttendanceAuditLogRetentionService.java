package com.bootsync.attendance.service;

import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.common.time.AppProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAuditLogRetentionService {

    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    public AttendanceAuditLogRetentionService(
        AttendanceAuditLogRepository attendanceAuditLogRepository,
        AppProperties appProperties,
        Clock clock
    ) {
        this.attendanceAuditLogRepository = attendanceAuditLogRepository;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Transactional
    public int clearExpiredRequestIpHmac() {
        LocalDateTime cutoff = LocalDateTime.now(clock)
            .minusDays(appProperties.getAudit().getRequestIpHmacRetentionDays());
        return attendanceAuditLogRepository.clearRequestIpHmacBefore(cutoff);
    }
}
