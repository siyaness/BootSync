package com.bootsync.attendance.service;

import com.bootsync.attendance.entity.AttendanceAuditAction;
import com.bootsync.attendance.entity.AttendanceAuditLog;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttendanceAuditLogRetentionServiceTest {

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

    @Autowired
    private AttendanceAuditLogRetentionService attendanceAuditLogRetentionService;

    @Autowired
    private Clock clock;

    @Test
    void clearExpiredRequestIpHmacNullsOnlyExpiredValues() {
        AttendanceAuditLog expiredLog = attendanceAuditLogRepository.save(createLog(
            LocalDateTime.now(clock).minusDays(31),
            "expired-hmac"
        ));
        AttendanceAuditLog recentLog = attendanceAuditLogRepository.save(createLog(
            LocalDateTime.now(clock).minusDays(5),
            "recent-hmac"
        ));
        AttendanceAuditLog alreadyNullLog = attendanceAuditLogRepository.save(createLog(
            LocalDateTime.now(clock).minusDays(40),
            null
        ));

        int cleared = attendanceAuditLogRetentionService.clearExpiredRequestIpHmac();

        Assertions.assertThat(cleared).isEqualTo(1);
        Assertions.assertThat(attendanceAuditLogRepository.findById(expiredLog.getId())).get().extracting(AttendanceAuditLog::getRequestIpHmac).isNull();
        Assertions.assertThat(attendanceAuditLogRepository.findById(recentLog.getId())).get().extracting(AttendanceAuditLog::getRequestIpHmac).isEqualTo("recent-hmac");
        Assertions.assertThat(attendanceAuditLogRepository.findById(alreadyNullLog.getId())).get().extracting(AttendanceAuditLog::getRequestIpHmac).isNull();
    }

    private AttendanceAuditLog createLog(LocalDateTime changedAt, String requestIpHmac) {
        AttendanceAuditLog log = new AttendanceAuditLog();
        log.setAction(AttendanceAuditAction.CREATE);
        log.setChangedAt(changedAt);
        log.setRequestIpHmac(requestIpHmac);
        return log;
    }
}
