package com.bootsync.member.service;

import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Profile({"local", "test"})
public class DemoAttendanceInitializer {

    @Bean
    @Order(15)
    ApplicationRunner demoAttendanceApplicationRunner(
        MemberRepository memberRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        Clock clock,
        @Value("${spring.security.user.name:d}") String demoUsername
    ) {
        return args -> {
            Long memberId = memberRepository.findByUsername(demoUsername)
                .map(member -> member.getId())
                .orElse(null);

            if (memberId == null || attendanceRecordRepository.existsByMemberId(memberId)) {
                return;
            }

            LocalDate today = LocalDate.now(clock);
            LocalDateTime now = LocalDateTime.now(clock);
            int startOffset = Math.min(4, today.getDayOfMonth() - 1);
            List<AttendanceRecord> records = new ArrayList<>();
            AttendanceStatus[] statuses = {
                AttendanceStatus.PRESENT,
                AttendanceStatus.LATE,
                AttendanceStatus.LEAVE_EARLY,
                AttendanceStatus.ABSENT,
                AttendanceStatus.PRESENT
            };
            String[] memos = {
                "정상 출석",
                "아침 지각",
                "조기 퇴실",
                "결석 처리",
                "오늘 체크 완료"
            };

            for (int offset = startOffset; offset >= 0; offset--) {
                int index = startOffset - offset;
                records.add(createRecord(
                    memberId,
                    today.minusDays(offset),
                    statuses[index],
                    memos[index],
                    now.minusDays(offset)
                ));
            }

            attendanceRecordRepository.saveAll(records);
        };
    }

    private AttendanceRecord createRecord(
        Long memberId,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String memo,
        LocalDateTime timestamp
    ) {
        AttendanceRecord record = new AttendanceRecord();
        record.setMemberId(memberId);
        record.setAttendanceDate(attendanceDate);
        record.setStatus(status);
        record.setMemo(memo);
        record.setCreatedAt(timestamp);
        record.setUpdatedAt(timestamp);
        return record;
    }
}
