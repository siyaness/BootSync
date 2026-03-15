package com.bootsync.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_audit_log")
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_record_id")
    private Long attendanceRecordId;

    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttendanceAuditAction action;

    @Column(name = "before_attendance_date")
    private LocalDate beforeAttendanceDate;

    @Column(name = "after_attendance_date")
    private LocalDate afterAttendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", length = 32)
    private AttendanceStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", length = 32)
    private AttendanceStatus afterStatus;

    @Column(name = "changed_by_member_id")
    private Long changedByMemberId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "request_ip_hmac", length = 64)
    private String requestIpHmac;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttendanceRecordId() {
        return attendanceRecordId;
    }

    public void setAttendanceRecordId(Long attendanceRecordId) {
        this.attendanceRecordId = attendanceRecordId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public AttendanceAuditAction getAction() {
        return action;
    }

    public void setAction(AttendanceAuditAction action) {
        this.action = action;
    }

    public LocalDate getBeforeAttendanceDate() {
        return beforeAttendanceDate;
    }

    public void setBeforeAttendanceDate(LocalDate beforeAttendanceDate) {
        this.beforeAttendanceDate = beforeAttendanceDate;
    }

    public LocalDate getAfterAttendanceDate() {
        return afterAttendanceDate;
    }

    public void setAfterAttendanceDate(LocalDate afterAttendanceDate) {
        this.afterAttendanceDate = afterAttendanceDate;
    }

    public AttendanceStatus getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(AttendanceStatus beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public AttendanceStatus getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(AttendanceStatus afterStatus) {
        this.afterStatus = afterStatus;
    }

    public Long getChangedByMemberId() {
        return changedByMemberId;
    }

    public void setChangedByMemberId(Long changedByMemberId) {
        this.changedByMemberId = changedByMemberId;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRequestIpHmac() {
        return requestIpHmac;
    }

    public void setRequestIpHmac(String requestIpHmac) {
        this.requestIpHmac = requestIpHmac;
    }
}
