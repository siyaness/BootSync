package com.bootsync.attendance.repository;

import com.bootsync.attendance.entity.AttendanceAuditLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, Long> {

    long countByMemberIdOrChangedByMemberId(Long memberId, Long changedByMemberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            update AttendanceAuditLog log
               set log.attendanceRecordId = null,
                   log.memberId = null,
                   log.changedByMemberId = null,
                   log.requestIpHmac = null
             where log.memberId = :memberId
                or log.changedByMemberId = :memberId
            """
    )
    int anonymizeByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            update AttendanceAuditLog log
               set log.requestIpHmac = null
             where log.requestIpHmac is not null
               and log.changedAt < :cutoff
            """
    )
    int clearRequestIpHmacBefore(@Param("cutoff") LocalDateTime cutoff);
}
