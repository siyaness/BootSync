package com.bootsync.attendance.repository;

import com.bootsync.attendance.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByMemberId(Long memberId);

    long countByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    Optional<AttendanceRecord> findByIdAndMemberId(Long id, Long memberId);

    Optional<AttendanceRecord> findByMemberIdAndAttendanceDate(Long memberId, LocalDate attendanceDate);

    List<AttendanceRecord> findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
        Long memberId,
        LocalDate startDate,
        LocalDate endDate
    );
}
