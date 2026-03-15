package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import com.bootsync.member.dto.TrainingProfileRules;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberTrainingProfile;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.MemberTrainingProfileRepository;
import com.bootsync.settings.dto.TrainingProfileRequest;
import com.bootsync.settings.dto.TrainingProfileResponse;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberTrainingProfileService {

    private static final List<DayOfWeek> DEFAULT_TRAINING_DAYS = List.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    );

    private static final List<DayOfWeek> ORDERED_TRAINING_DAYS = List.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    );

    private final MemberRepository memberRepository;
    private final MemberTrainingProfileRepository memberTrainingProfileRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    public MemberTrainingProfileService(
        MemberRepository memberRepository,
        MemberTrainingProfileRepository memberTrainingProfileRepository,
        AppProperties appProperties,
        Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberTrainingProfileRepository = memberTrainingProfileRepository;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TrainingProfileResponse profileFor(String username) {
        Long memberId = memberIdFor(username);
        return memberTrainingProfileRepository.findByMemberId(memberId)
            .map(profile -> toResponse(profile, true))
            .orElseGet(this::defaultResponse);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TrainingProfileRules> configuredRulesFor(String username) {
        return configuredRulesForMemberId(memberIdFor(username));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TrainingProfileRules> configuredRulesForMemberId(Long memberId) {
        return memberTrainingProfileRepository.findByMemberId(memberId)
            .map(this::toRules);
    }

    public TrainingProfileRules defaultRules() {
        return new TrainingProfileRules(
            null,
            appProperties.getTraining().getCourseStartDate(),
            appProperties.getTraining().getCourseEndDate(),
            appProperties.getTraining().getAttendanceThresholdPercent(),
            appProperties.getAllowance().getDailyAllowanceAmount(),
            appProperties.getAllowance().getPayableDayCap(),
            EnumSet.copyOf(DEFAULT_TRAINING_DAYS),
            List.copyOf(appProperties.getTraining().getHolidays())
        );
    }

    @Transactional
    public TrainingProfileResponse updateProfile(String username, TrainingProfileRequest request) {
        Long memberId = memberIdFor(username);
        String normalizedCourseLabel = normalizeCourseLabel(request.courseLabel());
        LocalDate courseStartDate = requiredDate(request.courseStartDate(), "courseStartDate", "과정 시작일을 입력해 주세요.");
        LocalDate courseEndDate = requiredDate(request.courseEndDate(), "courseEndDate", "과정 종료일을 입력해 주세요.");
        int thresholdPercent = requiredPositivePercent(request.attendanceThresholdPercent(), "attendanceThresholdPercent");
        int dailyAllowanceAmount = requiredNonNegativeNumber(request.dailyAllowanceAmount(), "dailyAllowanceAmount", "1일 지급액은 0원 이상이어야 합니다.");
        int payableDayCap = requiredPositiveNumber(request.payableDayCap(), "payableDayCap", "지급 상한 일수는 1일 이상이어야 합니다.");
        Set<DayOfWeek> trainingDays = parseTrainingDays(request.trainingDays());
        List<LocalDate> holidayDates = normalizeHolidayDates(request.holidayDates(), courseStartDate, courseEndDate);

        if (courseEndDate.isBefore(courseStartDate)) {
            throw new MemberValidationException("courseEndDate", "과정 종료일은 시작일보다 빠를 수 없습니다.");
        }

        MemberTrainingProfile profile = memberTrainingProfileRepository.findByMemberId(memberId)
            .orElseGet(MemberTrainingProfile::new);
        LocalDateTime now = LocalDateTime.now(clock);
        profile.setMemberId(memberId);
        profile.setCourseLabel(normalizedCourseLabel);
        profile.setCourseStartDate(courseStartDate);
        profile.setCourseEndDate(courseEndDate);
        profile.setAttendanceThresholdPercent(thresholdPercent);
        profile.setDailyAllowanceAmount(dailyAllowanceAmount);
        profile.setPayableDayCap(payableDayCap);
        profile.setTrainingDaysCsv(serializeTrainingDays(trainingDays));
        profile.setHolidayDatesCsv(serializeHolidayDates(holidayDates));
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(now);
        }
        profile.setUpdatedAt(now);

        return toResponse(memberTrainingProfileRepository.save(profile), true);
    }

    @Transactional
    public void clearProfile(String username) {
        memberTrainingProfileRepository.deleteByMemberId(memberIdFor(username));
    }

    private TrainingProfileRules toRules(MemberTrainingProfile profile) {
        return new TrainingProfileRules(
            profile.getCourseLabel(),
            profile.getCourseStartDate(),
            profile.getCourseEndDate(),
            profile.getAttendanceThresholdPercent(),
            profile.getDailyAllowanceAmount(),
            profile.getPayableDayCap(),
            parseTrainingDaysCsv(profile.getTrainingDaysCsv()),
            parseHolidayDatesCsv(profile.getHolidayDatesCsv())
        );
    }

    private TrainingProfileResponse toResponse(MemberTrainingProfile profile, boolean configured) {
        TrainingProfileRules rules = toRules(profile);
        return new TrainingProfileResponse(
            configured,
            rules.courseLabel(),
            rules.courseStartDate().toString(),
            rules.courseEndDate().toString(),
            rules.attendanceThresholdPercent(),
            rules.dailyAllowanceAmount(),
            rules.payableDayCap(),
            rules.maximumAllowanceAmount(),
            ORDERED_TRAINING_DAYS.stream()
                .filter(rules.trainingDays()::contains)
                .map(DayOfWeek::name)
                .toList(),
            rules.holidayDates().stream().map(LocalDate::toString).toList()
        );
    }

    private TrainingProfileResponse defaultResponse() {
        TrainingProfileRules rules = defaultRules();
        return new TrainingProfileResponse(
            false,
            null,
            rules.courseStartDate().toString(),
            rules.courseEndDate().toString(),
            rules.attendanceThresholdPercent(),
            rules.dailyAllowanceAmount(),
            rules.payableDayCap(),
            rules.maximumAllowanceAmount(),
            DEFAULT_TRAINING_DAYS.stream().map(DayOfWeek::name).toList(),
            rules.holidayDates().stream().map(LocalDate::toString).toList()
        );
    }

    private Long memberIdFor(String username) {
        return getRequiredMember(username).getId();
    }

    private Member getRequiredMember(String username) {
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다: " + username));
    }

    private String normalizeCourseLabel(String courseLabel) {
        if (!StringUtils.hasText(courseLabel)) {
            return null;
        }

        String normalized = courseLabel.trim();
        if (normalized.length() > 80) {
            throw new MemberValidationException("courseLabel", "과정 이름은 80자 이하여야 합니다.");
        }
        return normalized;
    }

    private LocalDate requiredDate(LocalDate value, String fieldName, String message) {
        if (value == null) {
            throw new MemberValidationException(fieldName, message);
        }
        return value;
    }

    private int requiredPositivePercent(Integer value, String fieldName) {
        if (value == null) {
            throw new MemberValidationException(fieldName, "수료 기준 출석률을 입력해 주세요.");
        }
        if (value < 1 || value > 100) {
            throw new MemberValidationException(fieldName, "수료 기준 출석률은 1에서 100 사이여야 합니다.");
        }
        return value;
    }

    private int requiredNonNegativeNumber(Integer value, String fieldName, String message) {
        if (value == null) {
            throw new MemberValidationException(fieldName, message);
        }
        if (value < 0) {
            throw new MemberValidationException(fieldName, message);
        }
        return value;
    }

    private int requiredPositiveNumber(Integer value, String fieldName, String message) {
        if (value == null) {
            throw new MemberValidationException(fieldName, message);
        }
        if (value < 1) {
            throw new MemberValidationException(fieldName, message);
        }
        return value;
    }

    private Set<DayOfWeek> parseTrainingDays(List<String> trainingDays) {
        if (trainingDays == null || trainingDays.isEmpty()) {
            throw new MemberValidationException("trainingDays", "수업 요일을 1개 이상 선택해 주세요.");
        }

        EnumSet<DayOfWeek> parsed = EnumSet.noneOf(DayOfWeek.class);
        for (String trainingDay : trainingDays) {
            if (!StringUtils.hasText(trainingDay)) {
                continue;
            }

            try {
                parsed.add(DayOfWeek.valueOf(trainingDay.trim().toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new MemberValidationException("trainingDays", "유효하지 않은 수업 요일이 포함되어 있습니다.");
            }
        }

        if (parsed.isEmpty()) {
            throw new MemberValidationException("trainingDays", "수업 요일을 1개 이상 선택해 주세요.");
        }
        return parsed;
    }

    private Set<DayOfWeek> parseTrainingDaysCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return EnumSet.copyOf(DEFAULT_TRAINING_DAYS);
        }
        return Arrays.stream(value.split(","))
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(DayOfWeek::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    private String serializeTrainingDays(Set<DayOfWeek> trainingDays) {
        return ORDERED_TRAINING_DAYS.stream()
            .filter(trainingDays::contains)
            .map(DayOfWeek::name)
            .collect(Collectors.joining(","));
    }

    private List<LocalDate> normalizeHolidayDates(List<LocalDate> holidayDates, LocalDate courseStartDate, LocalDate courseEndDate) {
        if (holidayDates == null || holidayDates.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<LocalDate> normalized = new LinkedHashSet<>();
        for (LocalDate holidayDate : holidayDates) {
            if (holidayDate == null) {
                continue;
            }
            if (holidayDate.isBefore(courseStartDate) || holidayDate.isAfter(courseEndDate)) {
                throw new MemberValidationException("holidayDates", "휴강일은 과정 기간 안에서만 등록할 수 있습니다.");
            }
            normalized.add(holidayDate);
        }
        return normalized.stream().sorted().toList();
    }

    private List<LocalDate> parseHolidayDatesCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(LocalDate::parse)
            .sorted()
            .toList();
    }

    private String serializeHolidayDates(List<LocalDate> holidayDates) {
        return holidayDates.stream()
            .map(LocalDate::toString)
            .collect(Collectors.joining(","));
    }
}
