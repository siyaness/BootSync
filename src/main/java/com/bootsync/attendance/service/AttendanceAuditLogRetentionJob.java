package com.bootsync.attendance.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "app.audit.request-ip-hmac-prune-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AttendanceAuditLogRetentionJob {

    private final AttendanceAuditLogRetentionService attendanceAuditLogRetentionService;

    public AttendanceAuditLogRetentionJob(AttendanceAuditLogRetentionService attendanceAuditLogRetentionService) {
        this.attendanceAuditLogRetentionService = attendanceAuditLogRetentionService;
    }

    @Scheduled(
        cron = "${app.audit.request-ip-hmac-prune-cron:0 25 3 * * *}",
        zone = "${app.timezone:Asia/Seoul}"
    )
    public void clearExpiredRequestIpHmac() {
        attendanceAuditLogRetentionService.clearExpiredRequestIpHmac();
    }
}
