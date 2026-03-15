package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberTrainingProfile;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.MemberTrainingProfileRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Profile({"local", "test"})
public class DemoTrainingProfileInitializer {

    @Bean
    @Order(12)
    ApplicationRunner demoTrainingProfileApplicationRunner(
        MemberRepository memberRepository,
        MemberTrainingProfileRepository memberTrainingProfileRepository,
        AppProperties appProperties,
        Clock clock,
        @Value("${spring.security.user.name:d}") String demoUsername
    ) {
        return args -> {
            Member member = memberRepository.findByUsername(demoUsername).orElse(null);
            if (member == null) {
                return;
            }

            MemberTrainingProfile profile = memberTrainingProfileRepository.findByMemberId(member.getId())
                .orElseGet(MemberTrainingProfile::new);
            LocalDateTime now = LocalDateTime.now(clock);
            profile.setMemberId(member.getId());
            profile.setCourseLabel("기본 국비 과정");
            profile.setCourseStartDate(appProperties.getTraining().getCourseStartDate());
            profile.setCourseEndDate(appProperties.getTraining().getCourseEndDate());
            profile.setAttendanceThresholdPercent(appProperties.getTraining().getAttendanceThresholdPercent());
            profile.setDailyAllowanceAmount(appProperties.getAllowance().getDailyAllowanceAmount());
            profile.setPayableDayCap(appProperties.getAllowance().getPayableDayCap());
            profile.setTrainingDaysCsv(List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ).stream().map(DayOfWeek::name).collect(Collectors.joining(",")));
            profile.setHolidayDatesCsv(appProperties.getTraining().getHolidays().stream()
                .map(java.time.LocalDate::toString)
                .collect(Collectors.joining(",")));
            if (profile.getCreatedAt() == null) {
                profile.setCreatedAt(now);
            }
            profile.setUpdatedAt(now);
            memberTrainingProfileRepository.save(profile);
        };
    }
}
