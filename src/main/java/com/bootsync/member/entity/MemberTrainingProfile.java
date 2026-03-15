package com.bootsync.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_training_profile")
public class MemberTrainingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "course_label", length = 80)
    private String courseLabel;

    @Column(name = "course_start_date", nullable = false)
    private LocalDate courseStartDate;

    @Column(name = "course_end_date", nullable = false)
    private LocalDate courseEndDate;

    @Column(name = "attendance_threshold_percent", nullable = false)
    private int attendanceThresholdPercent;

    @Column(name = "daily_allowance_amount", nullable = false)
    private int dailyAllowanceAmount;

    @Column(name = "payable_day_cap", nullable = false)
    private int payableDayCap;

    @Column(name = "training_days_csv", nullable = false, length = 64)
    private String trainingDaysCsv;

    @Column(name = "holiday_dates_csv", columnDefinition = "TEXT")
    private String holidayDatesCsv;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public void setCourseLabel(String courseLabel) {
        this.courseLabel = courseLabel;
    }

    public LocalDate getCourseStartDate() {
        return courseStartDate;
    }

    public void setCourseStartDate(LocalDate courseStartDate) {
        this.courseStartDate = courseStartDate;
    }

    public LocalDate getCourseEndDate() {
        return courseEndDate;
    }

    public void setCourseEndDate(LocalDate courseEndDate) {
        this.courseEndDate = courseEndDate;
    }

    public int getAttendanceThresholdPercent() {
        return attendanceThresholdPercent;
    }

    public void setAttendanceThresholdPercent(int attendanceThresholdPercent) {
        this.attendanceThresholdPercent = attendanceThresholdPercent;
    }

    public int getDailyAllowanceAmount() {
        return dailyAllowanceAmount;
    }

    public void setDailyAllowanceAmount(int dailyAllowanceAmount) {
        this.dailyAllowanceAmount = dailyAllowanceAmount;
    }

    public int getPayableDayCap() {
        return payableDayCap;
    }

    public void setPayableDayCap(int payableDayCap) {
        this.payableDayCap = payableDayCap;
    }

    public String getTrainingDaysCsv() {
        return trainingDaysCsv;
    }

    public void setTrainingDaysCsv(String trainingDaysCsv) {
        this.trainingDaysCsv = trainingDaysCsv;
    }

    public String getHolidayDatesCsv() {
        return holidayDatesCsv;
    }

    public void setHolidayDatesCsv(String holidayDatesCsv) {
        this.holidayDatesCsv = holidayDatesCsv;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
