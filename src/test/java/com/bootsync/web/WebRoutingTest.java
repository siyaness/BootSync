package com.bootsync.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsync.attendance.entity.AttendanceAuditAction;
import com.bootsync.attendance.entity.AttendanceRecord;
import com.bootsync.attendance.entity.AttendanceStatus;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.attendance.repository.AttendanceRecordRepository;
import com.bootsync.config.ActiveMemberSessionFilter;
import com.bootsync.config.InMemoryRateLimitService;
import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.repository.MemberTrainingProfileRepository;
import com.bootsync.member.security.BootSyncPrincipal;
import com.bootsync.member.repository.RecoveryEmailVerificationTokenRepository;
import com.bootsync.member.service.AccountDeletionService;
import com.bootsync.member.service.MemberUserDetailsService;
import com.bootsync.member.service.OperatorPasswordResetService;
import com.bootsync.member.service.RecoveryEmailVerificationPreviewStore;
import com.bootsync.member.service.RecoveryEmailVerificationService;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.entity.Tag;
import com.bootsync.tag.repository.TagRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberTrainingProfileRepository memberTrainingProfileRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

    @Autowired
    private RecoveryEmailVerificationTokenRepository recoveryEmailVerificationTokenRepository;

    @Autowired
    private RecoveryEmailVerificationPreviewStore recoveryEmailVerificationPreviewStore;

    @Autowired
    private RecoveryEmailVerificationService recoveryEmailVerificationService;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private SnippetTagRepository snippetTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRateLimitService inMemoryRateLimitService;

    @Autowired
    private MemberUserDetailsService memberUserDetailsService;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private OperatorPasswordResetService operatorPasswordResetService;

    @BeforeEach
    void resetRateLimits() {
        inMemoryRateLimitService.clearAll();
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login"));
    }

    @Test
    void apiSessionIsPublicForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(false))
            .andExpect(jsonPath("$.csrf.token").isNotEmpty());
    }

    @Test
    void prometheusEndpointRejectsAnonymousScrape() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusEndpointAcceptsConfiguredBearerToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                .header("Authorization", "Bearer test-prometheus-token"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    @Test
    void actuatorHealthProbeEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));

        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    void apiLoginCreatesSessionAndApiSessionReturnsCurrentUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Member demoMember = memberRepository.findByUsername("d").orElseThrow();

        MockHttpSession loginRequestSession = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "d",
                      "password": "d"
                    }
                    """))
            .andExpect(status().isNoContent())
            .andReturn()
            .getRequest()
            .getSession(false);

        Assertions.assertThat(loginRequestSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();

        mockMvc.perform(get("/api/auth/session").with(user(BootSyncPrincipal.from(demoMember))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.user.username").value("d"))
            .andExpect(jsonPath("$.user.displayName").value("d"));
    }

    @Test
    void apiProfileUpdateRefreshesSessionDisplayName() throws Exception {
        Member demoMember = memberRepository.findByUsername("d").orElseThrow();

        mockMvc.perform(patch("/api/settings/profile")
                .with(user(BootSyncPrincipal.from(demoMember)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName": "새 표시 이름"
                    }
                    """))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/session")
                .with(user(BootSyncPrincipal.from(memberRepository.findByUsername("d").orElseThrow()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.user.displayName").value("새 표시 이름"));
    }

    @Test
    void apiSessionIncludesPendingRecoveryEmailStatusForFrontend() throws Exception {
        Member member = createMember("api_pending_user", "pending-password", "인증 대기 사용자");
        recoveryEmailVerificationService.issueSignupVerification(member, "pending@example.com");

        mockMvc.perform(get("/api/auth/session")
                .with(user(BootSyncPrincipal.from(memberRepository.findByUsername("api_pending_user").orElseThrow()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.recoveryEmailStatus.hasPendingVerification").value(true))
            .andExpect(jsonPath("$.recoveryEmailStatus.pendingPurposeLabel").value("회원가입 복구 이메일 인증"))
            .andExpect(jsonPath("$.recoveryEmailStatus.maskedPendingRecoveryEmail").isNotEmpty())
            .andExpect(jsonPath("$.recoveryEmailStatus.developmentPreviewPath").value(org.hamcrest.Matchers.containsString("/app/verify-email?purpose=signup&token=")));
    }

    @Test
    void appFrontendShellIsPublic() throws Exception {
        ResultActions result = mockMvc.perform(get("/app"));
        if (frontendShellAvailable()) {
            result.andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));
            return;
        }
        result.andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("최신 프론트 빌드가 필요합니다")));
    }

    @Test
    void appFrontendChildRouteForwardsToFrontendShell() throws Exception {
        ResultActions result = mockMvc.perform(get("/app/dashboard"));
        if (frontendShellAvailable()) {
            result.andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));
            return;
        }
        result.andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("최신 프론트 빌드가 필요합니다")));
    }

    @Test
    void appFrontendCourseStatusRouteForwardsToFrontendShell() throws Exception {
        ResultActions result = mockMvc.perform(get("/app/course-status"));
        if (frontendShellAvailable()) {
            result.andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));
            return;
        }
        result.andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("최신 프론트 빌드가 필요합니다")));
    }

    @Test
    void homeRedirectsAnonymousUsersToAppLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login"));
    }

    @Test
    void homeRedirectsAuthenticatedUsersToAppDashboard() throws Exception {
        mockMvc.perform(get("/").with(user("d")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));
    }

    @Test
    void dashboardRedirectsWhenAnonymous() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginRedirectsToOriginalProtectedPage() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Snippet snippet = snippetRepository.save(createSnippet(demoMemberId, "saved-request-note", "saved-request-content"));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/snippets/" + snippet.getId()).session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(post("/auth/login")
                .session(session)
                .with(csrf())
                .param("username", "d")
                .param("password", "d"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/snippets/" + snippet.getId() + "?continue"));
    }

    @Test
    void dashboardRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("d")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));
    }

    @Test
    void inactiveMemberSessionIsLoggedOutOnNextProtectedRequest() throws Exception {
        MockHttpSession session = copySession((MockHttpSession) mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "d")
                .param("password", "d"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getRequest()
            .getSession(false));

        Member demoMember = memberRepository.findByUsername("d").orElseThrow();
        demoMember.setStatus(MemberStatus.PENDING_DELETE);
        memberRepository.save(demoMember);

        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?reason=pending_delete"));
    }

    @Test
    void inactiveMemberSessionReturnsJsonUnauthorizedForApiRequests() throws Exception {
        MockHttpSession session = loginSession("d", "d");

        Member demoMember = memberRepository.findByUsername("d").orElseThrow();
        demoMember.setStatus(MemberStatus.PENDING_DELETE);
        memberRepository.save(demoMember);

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("pending_delete"))
            .andExpect(jsonPath("$.message").value("계정 삭제 요청이 접수되어 현재 세션이 종료되었습니다."));
    }

    @Test
    void dashboardUsesCurrentMembersAttendanceSummaryOnly() throws Exception {
        Member member = createMember("attendance_user");
        Member otherMember = createMember("attendance_other");
        LocalDate today = LocalDate.now(clock);

        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today.minusDays(4), AttendanceStatus.PRESENT, "visible-present"));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today.minusDays(3), AttendanceStatus.LATE, "visible-late-1"));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today.minusDays(2), AttendanceStatus.LATE, "visible-late-2"));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today.minusDays(1), AttendanceStatus.LATE, "visible-late-3"));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today, AttendanceStatus.ABSENT, "visible-absent"));
        attendanceRecordRepository.save(createAttendanceRecord(otherMember.getId(), today, AttendanceStatus.PRESENT, "hidden-present"));

        mockMvc.perform(get("/api/attendance")
                .with(user("attendance_user"))
                .param("yearMonth", YearMonth.from(today).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlySummary.presentCount").value(1))
            .andExpect(jsonPath("$.monthlySummary.lateCount").value(3))
            .andExpect(jsonPath("$.monthlySummary.absentCount").value(1))
            .andExpect(jsonPath("$.monthlySummary.expectedAllowanceAmount").value(47400))
            .andExpect(jsonPath("$.allowanceSummary.dailyAllowanceAmount").value(15800))
            .andExpect(jsonPath("$.trainingSummary").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hidden-present"))));
    }

    @Test
    void trainingProfileEndpointReturnsDefaultsWhenMemberHasNotConfiguredCourse() throws Exception {
        createMember("training_blank", "training-blank-password", "과정 미설정 사용자");

        mockMvc.perform(get("/api/settings/training-profile").with(user("training_blank")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.courseStartDate").value("2025-09-23"))
            .andExpect(jsonPath("$.courseEndDate").value("2026-03-27"))
            .andExpect(jsonPath("$.dailyAllowanceAmount").value(15800))
            .andExpect(jsonPath("$.payableDayCap").value(20))
            .andExpect(jsonPath("$.maximumAllowanceAmount").value(316000))
            .andExpect(jsonPath("$.trainingDays[0]").value("MONDAY"));
    }

    @Test
    void trainingProfileUpdatePersonalizesAttendanceSummary() throws Exception {
        Member member = createMember("training_owner", "training-owner-password", "개인 과정 사용자");
        LocalDate trainingDate = LocalDate.of(2026, 3, 3);

        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), trainingDate, AttendanceStatus.ABSENT, "custom-absent"));

        mockMvc.perform(put("/api/settings/training-profile")
                .with(user("training_owner"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseLabel": "DevOps 9기",
                      "courseStartDate": "2026-03-02",
                      "courseEndDate": "2026-03-13",
                      "attendanceThresholdPercent": 75,
                      "dailyAllowanceAmount": 15000,
                      "payableDayCap": 20,
                      "trainingDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                      "holidayDates": ["2026-03-06"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.courseLabel").value("DevOps 9기"))
            .andExpect(jsonPath("$.dailyAllowanceAmount").value(15000))
            .andExpect(jsonPath("$.payableDayCap").value(20))
            .andExpect(jsonPath("$.maximumAllowanceAmount").value(300000));

        mockMvc.perform(get("/api/attendance")
                .with(user("training_owner"))
                .param("yearMonth", "2026-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlySummary.baseAllowanceAmount").value(300000))
            .andExpect(jsonPath("$.monthlySummary.absenceDeductionAmount").value(15000))
            .andExpect(jsonPath("$.monthlySummary.expectedAllowanceAmount").value(0))
            .andExpect(jsonPath("$.allowanceSummary.dailyAllowanceAmount").value(15000))
            .andExpect(jsonPath("$.allowanceSummary.payableDayCap").value(20))
            .andExpect(jsonPath("$.allowanceSummary.maximumAllowanceAmount").value(300000))
            .andExpect(jsonPath("$.trainingSummary.courseLabel").value("DevOps 9기"))
            .andExpect(jsonPath("$.trainingSummary.thresholdPercent").value(75))
            .andExpect(jsonPath("$.trainingSummary.monthHolidayDates[0]").value("2026-03-06"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/settings/training-profile")
                .with(user("training_owner"))
                .with(csrf()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/attendance")
                .with(user("training_owner"))
                .param("yearMonth", "2026-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlySummary.baseAllowanceAmount").value(316000))
            .andExpect(jsonPath("$.allowanceSummary.dailyAllowanceAmount").value(15800))
            .andExpect(jsonPath("$.trainingSummary").value(org.hamcrest.Matchers.nullValue()));

        Assertions.assertThat(memberTrainingProfileRepository.countByMemberId(member.getId())).isZero();
    }

    @Test
    void memberUserDetailsServiceReturnsCustomPrincipalWithMemberFields() {
        Member member = createMember("principal_user", "principal-password", "표시 이름 사용자");

        org.springframework.security.core.userdetails.UserDetails userDetails =
            memberUserDetailsService.loadUserByUsername("principal_user");

        Assertions.assertThat(userDetails).isInstanceOf(BootSyncPrincipal.class);
        BootSyncPrincipal principal = (BootSyncPrincipal) userDetails;
        Assertions.assertThat(principal.memberId()).isEqualTo(member.getId());
        Assertions.assertThat(principal.getUsername()).isEqualTo("principal_user");
        Assertions.assertThat(principal.displayName()).isEqualTo("표시 이름 사용자");
    }

    @Test
    void dashboardUsesDisplayNameFromCustomPrincipalAfterLogin() throws Exception {
        createMember("display_user", "display-password", "표시 이름 사용자");

        MockHttpSession session = copySession((MockHttpSession) mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "display_user")
                .param("password", "display-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"))
            .andReturn()
            .getRequest()
            .getSession(false));

        mockMvc.perform(get("/api/auth/session")
                .with(user(BootSyncPrincipal.from(memberRepository.findByUsername("display_user").orElseThrow()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.displayName").value("표시 이름 사용자"));
    }

    @Test
    void snippetSearchKeepsQueryParametersInForm() throws Exception {
        mockMvc.perform(
                get("/snippets")
                    .with(user("d"))
                    .param("q", "hello")
                    .param("tag", "spring")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/snippets?q=hello&tag=spring"));
    }

    @Test
    void snippetSearchShowsOnlyCurrentMembersResults() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Member otherMember = createMember("other_user");

        snippetRepository.save(createSnippet(demoMemberId, "demo-visible-note", "demo-visible-content"));
        snippetRepository.save(createSnippet(otherMember.getId(), "other-hidden-note", "other-hidden-content"));

        mockMvc.perform(get("/api/snippets").with(user("d")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("demo-visible-note")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("other-hidden-note"))));
    }

    @Test
    void snippetSearchFiltersByTagForCurrentMember() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Member otherMember = createMember("tag_other_user");

        Tag demoTag = createTag(demoMemberId, "focus");
        Tag otherTag = createTag(otherMember.getId(), "focus");

        Snippet demoSnippet = snippetRepository.save(createSnippet(demoMemberId, "focus-demo-note", "focus-demo-content"));
        Snippet otherSnippet = snippetRepository.save(createSnippet(otherMember.getId(), "focus-other-note", "focus-other-content"));

        snippetTagRepository.save(link(demoMemberId, demoSnippet.getId(), demoTag.getId()));
        snippetTagRepository.save(link(otherMember.getId(), otherSnippet.getId(), otherTag.getId()));

        mockMvc.perform(get("/snippets").with(user("d")).param("tag", "focus"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/snippets?tag=focus"));
    }

    @Test
    void snippetSearchShowsValidationErrorInsteadOfReturning400() throws Exception {
        String tooLongQuery = "a".repeat(101);

        mockMvc.perform(get("/snippets").with(user("d")).param("q", tooLongQuery))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/snippets?q=" + tooLongQuery));
    }

    @Test
    void snippetDetailReturns404WhenSnippetDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/snippets/999999").with(user("d")))
            .andExpect(status().isNotFound());
    }

    @Test
    void snippetDetailRendersWhenSnippetExists() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();

        Snippet snippet = new Snippet();
        snippet.setMemberId(demoMemberId);
        snippet.setTitle("Spring Security note");
        snippet.setContentMarkdown("Content");
        snippet.setCreatedAt(LocalDateTime.now());
        snippet.setUpdatedAt(LocalDateTime.now());

        Snippet savedSnippet = snippetRepository.save(snippet);

        mockMvc.perform(get("/api/snippets/" + savedSnippet.getId()).with(user("d")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Spring Security note"))
            .andExpect(jsonPath("$.content").value("Content"));
    }

    @Test
    void snippetDetailRouteRedirectsToApp() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();

        Snippet snippet = snippetRepository.save(
            createSnippet(
                demoMemberId,
                "Markdown 렌더링",
                "# Heading\n\n**bold** text\n\n<script>alert('xss')</script>\n\n- item"
            )
        );

        mockMvc.perform(get("/snippets/" + snippet.getId()).with(user("d")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/snippets/" + snippet.getId()));
    }

    @Test
    void snippetDetailReturns404ForOtherMembersSnippet() throws Exception {
        Member otherMember = createMember("detail_other_user");

        Snippet snippet = createSnippet(otherMember.getId(), "Other member note", "Secret");

        Snippet savedSnippet = snippetRepository.save(snippet);

        mockMvc.perform(get("/api/snippets/" + savedSnippet.getId()).with(user("d")))
            .andExpect(status().isNotFound());
    }

    @Test
    void attendancePageShowsCurrentMembersMonthlyRecordsOnly() throws Exception {
        Member member = createMember("attendance_page_user");
        Member otherMember = createMember("att_page_other");
        LocalDate today = LocalDate.now(clock);

        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today.minusDays(1), AttendanceStatus.LATE, "visible attendance memo"));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), today, AttendanceStatus.PRESENT, "today attendance memo"));
        attendanceRecordRepository.save(createAttendanceRecord(otherMember.getId(), today, AttendanceStatus.ABSENT, "hidden attendance memo"));

        mockMvc.perform(get("/api/attendance")
                .with(user("attendance_page_user"))
                .param("yearMonth", YearMonth.from(today).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records.length()").value(2))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("visible attendance memo")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("today attendance memo")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hidden attendance memo"))));
    }

    @Test
    void attendancePageCanRenderPreviousMonthView() throws Exception {
        Member member = createMember("att_prev_view");
        LocalDate previousMonthDate = LocalDate.now(clock).minusMonths(1).withDayOfMonth(3);

        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), previousMonthDate, AttendanceStatus.LEAVE_EARLY, "previous month visible"));

        mockMvc.perform(get("/attendance")
                .with(user("att_prev_view"))
                .param("yearMonth", YearMonth.from(previousMonthDate).toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/attendance?yearMonth=" + YearMonth.from(previousMonthDate)));
    }

    @Test
    void attendancePostCreatesRecordAndAuditLog() throws Exception {
        Member member = createMember("att_save_user");
        LocalDate today = LocalDate.now(clock);

        mockMvc.perform(put("/api/attendance/" + today)
                .with(user("att_save_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "지각",
                      "memo": "saved attendance memo"
                    }
                    """))
            .andExpect(status().isOk());

        AttendanceRecord savedRecord = attendanceRecordRepository.findByMemberIdAndAttendanceDate(member.getId(), today)
            .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(savedRecord.getStatus()).isEqualTo(AttendanceStatus.LATE);
        org.assertj.core.api.Assertions.assertThat(savedRecord.getMemo()).isEqualTo("saved attendance memo");
        org.assertj.core.api.Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .anySatisfy(log -> {
                org.assertj.core.api.Assertions.assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CREATE);
                org.assertj.core.api.Assertions.assertThat(log.getMemberId()).isEqualTo(member.getId());
                org.assertj.core.api.Assertions.assertThat(log.getAttendanceRecordId()).isEqualTo(savedRecord.getId());
            });
    }

    @Test
    void attendancePostUpdatesExistingRecordForSameDate() throws Exception {
        Member member = createMember("att_update_user");
        LocalDate today = LocalDate.now(clock);

        AttendanceRecord existingRecord = attendanceRecordRepository.save(
            createAttendanceRecord(member.getId(), today, AttendanceStatus.PRESENT, "before memo")
        );

        mockMvc.perform(put("/api/attendance/" + today)
                .with(user("att_update_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "결석",
                      "memo": "after memo"
                    }
                    """))
            .andExpect(status().isOk());

        AttendanceRecord updatedRecord = attendanceRecordRepository.findByMemberIdAndAttendanceDate(member.getId(), today)
            .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(attendanceRecordRepository.countByMemberId(member.getId())).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(updatedRecord.getId()).isEqualTo(existingRecord.getId());
        org.assertj.core.api.Assertions.assertThat(updatedRecord.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        org.assertj.core.api.Assertions.assertThat(updatedRecord.getMemo()).isEqualTo("after memo");
        org.assertj.core.api.Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .anySatisfy(log -> {
                org.assertj.core.api.Assertions.assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.UPDATE);
                org.assertj.core.api.Assertions.assertThat(log.getAttendanceRecordId()).isEqualTo(existingRecord.getId());
                org.assertj.core.api.Assertions.assertThat(log.getMemberId()).isEqualTo(member.getId());
            });
    }

    @Test
    void apiAttendanceUpsertRejectsFutureDate() throws Exception {
        Member member = createMember("att_api_future_user", "att-api-future-password", "API 미래 사용자");
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        mockMvc.perform(put("/api/attendance/" + tomorrow)
                .with(user(BootSyncPrincipal.from(member)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "출석",
                      "memo": "future memo"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.attendanceDate").value("미래 날짜 출결은 저장할 수 없습니다."));

        Assertions.assertThat(attendanceRecordRepository.findByMemberIdAndAttendanceDate(member.getId(), tomorrow)).isEmpty();
        Assertions.assertThat(attendanceAuditLogRepository.findAll()).noneMatch(log -> member.getId().equals(log.getMemberId()));
    }

    @Test
    void apiAttendanceUpsertSavesPreviousMonthDate() throws Exception {
        Member member = createMember("att_prev_user");
        LocalDate previousMonthDate = LocalDate.now(clock).minusMonths(1).withDayOfMonth(1);

        mockMvc.perform(put("/api/attendance/" + previousMonthDate)
                .with(user("att_prev_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "출석",
                      "memo": "previous month memo"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.yearMonth").value(YearMonth.from(previousMonthDate).toString()));

        AttendanceRecord savedRecord = attendanceRecordRepository.findByMemberIdAndAttendanceDate(member.getId(), previousMonthDate)
            .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(savedRecord.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        org.assertj.core.api.Assertions.assertThat(savedRecord.getMemo()).isEqualTo("previous month memo");
    }

    @Test
    void apiAttendanceBulkFillRequiresConfiguredTrainingProfile() throws Exception {
        createMember("att_bulk_missing");

        mockMvc.perform(post("/api/attendance/bulk-fill/present")
                .with(user("att_bulk_missing"))
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.trainingProfile").value("과정 현황에서 먼저 내 과정 정보를 저장해 주세요."));
    }

    @Test
    void apiAttendanceBulkFillPreviewShowsExactEligibleDayCount() throws Exception {
        Member member = createMember("att_bulk_preview", "att-bulk-preview", "일괄 미리보기 사용자");
        LocalDate today = LocalDate.now(clock);
        LocalDate courseStartDate = today.minusDays(10);
        LocalDate courseEndDate = today.plusDays(14);
        Set<DayOfWeek> trainingDays = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );
        LocalDate holidayDate = firstMatchingDate(courseStartDate, today, trainingDays, Set.of());
        LocalDate existingDate = firstMatchingDate(holidayDate.plusDays(1), today, trainingDays, Set.of(holidayDate));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), existingDate, AttendanceStatus.ABSENT, "existing preview record"));

        mockMvc.perform(put("/api/settings/training-profile")
                .with(user("att_bulk_preview"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseLabel": "일괄 미리보기 테스트",
                      "courseStartDate": "%s",
                      "courseEndDate": "%s",
                      "attendanceThresholdPercent": 80,
                      "dailyAllowanceAmount": 15800,
                      "payableDayCap": 20,
                      "trainingDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                      "holidayDates": ["%s"]
                    }
                    """.formatted(courseStartDate, courseEndDate, holidayDate)))
            .andExpect(status().isOk());

        int expectedCreatedCount = countEligibleTrainingDays(
            courseStartDate,
            today,
            trainingDays,
            Set.of(holidayDate),
            Set.of(existingDate)
        );

        mockMvc.perform(get("/api/attendance/bulk-fill/present/preview").with(user("att_bulk_preview")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startDate").value(courseStartDate.toString()))
            .andExpect(jsonPath("$.endDate").value(today.toString()))
            .andExpect(jsonPath("$.createdCount").value(expectedCreatedCount));

        Assertions.assertThat(
                attendanceRecordRepository.findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                    member.getId(),
                    courseStartDate,
                    today
                )
            )
            .hasSize(1)
            .allSatisfy(record -> Assertions.assertThat(record.getAttendanceDate()).isEqualTo(existingDate));
    }

    @Test
    void apiAttendanceBulkFillCreatesPresentRecordsOnlyForBlankConfiguredTrainingDays() throws Exception {
        Member member = createMember("att_bulk_user", "att-bulk-password", "일괄 입력 사용자");
        LocalDate today = LocalDate.now(clock);
        LocalDate courseStartDate = today.minusDays(12);
        LocalDate courseEndDate = today.plusDays(20);
        Set<DayOfWeek> trainingDays = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );
        LocalDate holidayDate = firstMatchingDate(courseStartDate, today, trainingDays, Set.of());
        LocalDate existingDate = firstMatchingDate(holidayDate.plusDays(1), today, trainingDays, Set.of(holidayDate));
        attendanceRecordRepository.save(createAttendanceRecord(member.getId(), existingDate, AttendanceStatus.LATE, "existing record"));

        mockMvc.perform(put("/api/settings/training-profile")
                .with(user("att_bulk_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseLabel": "일괄 입력 테스트",
                      "courseStartDate": "%s",
                      "courseEndDate": "%s",
                      "attendanceThresholdPercent": 80,
                      "dailyAllowanceAmount": 15800,
                      "payableDayCap": 20,
                      "trainingDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                      "holidayDates": ["%s"]
                    }
                    """.formatted(courseStartDate, courseEndDate, holidayDate)))
            .andExpect(status().isOk());

        int expectedCreatedCount = countEligibleTrainingDays(
            courseStartDate,
            today,
            trainingDays,
            Set.of(holidayDate),
            Set.of(existingDate)
        );

        mockMvc.perform(post("/api/attendance/bulk-fill/present")
                .with(user("att_bulk_user"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startDate").value(courseStartDate.toString()))
            .andExpect(jsonPath("$.endDate").value(today.toString()))
            .andExpect(jsonPath("$.createdCount").value(expectedCreatedCount));

        Assertions.assertThat(
                attendanceRecordRepository.findByMemberIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                    member.getId(),
                    courseStartDate,
                    today
                )
            )
            .hasSize(expectedCreatedCount + 1)
            .allSatisfy(record -> {
                if (record.getAttendanceDate().isEqual(existingDate)) {
                    Assertions.assertThat(record.getStatus()).isEqualTo(AttendanceStatus.LATE);
                    Assertions.assertThat(record.getMemo()).isEqualTo("existing record");
                    return;
                }
                Assertions.assertThat(record.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
                Assertions.assertThat(record.getMemo()).isNull();
            })
            .noneMatch(record -> record.getAttendanceDate().isEqual(holidayDate));

        Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .filteredOn(log -> member.getId().equals(log.getMemberId()))
            .hasSize(expectedCreatedCount)
            .allSatisfy(log -> Assertions.assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CREATE));
    }

    @Test
    void attendanceEditModeLoadsOwnedRecordIntoForm() throws Exception {
        Member member = createMember("att_edit_page_user");
        LocalDate targetDate = LocalDate.now(clock).minusDays(2);
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(member.getId(), targetDate, AttendanceStatus.LEAVE_EARLY, "edit target memo")
        );

        mockMvc.perform(get("/attendance")
                .with(user("att_edit_page_user"))
                .param("editId", record.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/attendance?editId=" + record.getId()));
    }

    @Test
    void apiAttendanceRecordReturnsOwnedRecordForLegacyEditContext() throws Exception {
        Member member = createMember("att_edit_api_user");
        LocalDate targetDate = LocalDate.now(clock).minusDays(4);
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(member.getId(), targetDate, AttendanceStatus.LATE, "legacy edit memo")
        );

        mockMvc.perform(get("/api/attendance/record/" + record.getId()).with(user("att_edit_api_user")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(record.getId()))
            .andExpect(jsonPath("$.date").value(targetDate.toString()))
            .andExpect(jsonPath("$.status").value("지각"))
            .andExpect(jsonPath("$.memo").value("legacy edit memo"));
    }

    @Test
    void apiAttendanceRecordReturns404ForOtherMembersRecord() throws Exception {
        Member owner = createMember("att_record_owner");
        Member otherMember = createMember("att_record_other");
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(owner.getId(), LocalDate.now(clock).minusDays(2), AttendanceStatus.PRESENT, "hidden record")
        );

        mockMvc.perform(get("/api/attendance/record/" + record.getId()).with(user(otherMember.getUsername())))
            .andExpect(status().isNotFound());
    }

    @Test
    void apiAttendanceUpsertUpdatesExistingRecordAndWritesAuditLog() throws Exception {
        Member member = createMember("att_patch_user");
        LocalDate targetDate = LocalDate.now(clock).minusDays(3);
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(member.getId(), targetDate, AttendanceStatus.PRESENT, "before patch")
        );

        mockMvc.perform(put("/api/attendance/" + targetDate)
                .with(user("att_patch_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "결석",
                      "memo": "after patch"
                    }
                    """))
            .andExpect(status().isOk());

        AttendanceRecord updatedRecord = attendanceRecordRepository.findByIdAndMemberId(record.getId(), member.getId())
            .orElseThrow();

        Assertions.assertThat(updatedRecord.getAttendanceDate()).isEqualTo(targetDate);
        Assertions.assertThat(updatedRecord.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        Assertions.assertThat(updatedRecord.getMemo()).isEqualTo("after patch");
        Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .anySatisfy(log -> {
                Assertions.assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.UPDATE);
                Assertions.assertThat(log.getAttendanceRecordId()).isEqualTo(record.getId());
                Assertions.assertThat(log.getBeforeAttendanceDate()).isEqualTo(targetDate);
                Assertions.assertThat(log.getAfterAttendanceDate()).isEqualTo(targetDate);
                Assertions.assertThat(log.getBeforeStatus()).isEqualTo(AttendanceStatus.PRESENT);
                Assertions.assertThat(log.getAfterStatus()).isEqualTo(AttendanceStatus.ABSENT);
            });
    }

    @Test
    void attendanceDeleteRemovesOwnedRecordAndWritesDeleteAuditLog() throws Exception {
        Member member = createMember("att_delete_user");
        LocalDate targetDate = LocalDate.now(clock).minusDays(4);
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(member.getId(), targetDate, AttendanceStatus.LATE, "delete target")
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/attendance/" + record.getId())
                .with(user("att_delete_user"))
                .with(csrf())
                )
            .andExpect(status().isOk());

        Assertions.assertThat(attendanceRecordRepository.findByIdAndMemberId(record.getId(), member.getId())).isEmpty();
        Assertions.assertThat(attendanceAuditLogRepository.findAll())
            .anySatisfy(log -> {
                Assertions.assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.DELETE);
                Assertions.assertThat(log.getAttendanceRecordId()).isEqualTo(record.getId());
                Assertions.assertThat(log.getBeforeAttendanceDate()).isEqualTo(targetDate);
                Assertions.assertThat(log.getAfterAttendanceDate()).isNull();
                Assertions.assertThat(log.getBeforeStatus()).isEqualTo(AttendanceStatus.LATE);
                Assertions.assertThat(log.getAfterStatus()).isNull();
            });
    }

    @Test
    void attendanceDeleteReturns404ForOtherMembersRecord() throws Exception {
        Member owner = createMember("att_owner_user");
        createMember("att_intruder_user");
        AttendanceRecord record = attendanceRecordRepository.save(
            createAttendanceRecord(owner.getId(), LocalDate.now(clock).minusDays(1), AttendanceStatus.PRESENT, "owner memo")
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/attendance/" + record.getId())
                .with(user("att_intruder_user"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        Assertions.assertThat(attendanceRecordRepository.findByIdAndMemberId(record.getId(), owner.getId())).isPresent();
    }

    @Test
    void signupCreatesMemberAndAllowsLogin() throws Exception {
        MockHttpSession signupSession = copySession((MockHttpSession) mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "fresh_user",
                      "password": "fresh-password",
                      "displayName": "새사용자",
                      "recoveryEmail": "fresh@example.com"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getRequest()
            .getSession(false));

        Member savedMember = memberRepository.findByUsername("fresh_user")
            .orElseThrow();

        Assertions.assertThat(savedMember.getDisplayName()).isEqualTo("새사용자");
        Assertions.assertThat(savedMember.getRecoveryEmail()).isNull();
        Assertions.assertThat(savedMember.getRecoveryEmailVerifiedAt()).isNull();
        Assertions.assertThat(passwordEncoder.matches("fresh-password", savedMember.getPasswordHash())).isTrue();
        Assertions.assertThat(
            recoveryEmailVerificationTokenRepository
                .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                    savedMember.getId(),
                    RecoveryEmailVerificationPurpose.SIGNUP_VERIFY
                )
        ).isPresent();

        Assertions.assertThat(signupSession.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
        Assertions.assertThat(signupSession.getAttribute(ActiveMemberSessionFilter.ACTIVE_MEMBER_REVALIDATION_MARKER))
            .isEqualTo(Boolean.TRUE);

        mockMvc.perform(get("/dashboard")
                .with(user(BootSyncPrincipal.from(savedMember))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));

        mockMvc.perform(get("/api/auth/session")
                .with(user(BootSyncPrincipal.from(savedMember))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.user.username").value("fresh_user"));
    }

    @Test
    void signupRejectsDuplicateUsernameAndRecoveryEmail() throws Exception {
        Member duplicateEmailMember = createMember("email_taken");
        duplicateEmailMember.setRecoveryEmail("taken@example.com");
        memberRepository.save(duplicateEmailMember);

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "email_taken",
                      "password": "duplicate-pass",
                      "displayName": "중복사용자",
                      "recoveryEmail": "TAKEN@example.com"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.username").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    void signupRejectsCommonPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "weak_signup_user",
                      "password": "password1234",
                      "displayName": "약한비밀번호",
                      "recoveryEmail": "weak@example.com"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("너무 흔한 비밀번호는 사용할 수 없습니다."));

        Assertions.assertThat(memberRepository.findByUsername("weak_signup_user")).isEmpty();
    }

    @Test
    void recoveryEmailPreviewDoesNotConsumeTokenAndConfirmPromotesPendingTarget() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "verify_user",
                      "password": "verify-password",
                      "displayName": "검증사용자",
                      "recoveryEmail": "verify@example.com"
                    }
                    """))
            .andExpect(status().isCreated());

        Member savedMember = memberRepository.findByUsername("verify_user")
            .orElseThrow();

        RecoveryEmailVerificationPreviewLink previewLink = recoveryEmailVerificationPreviewStore
            .find(savedMember.getId(), RecoveryEmailVerificationPurpose.SIGNUP_VERIFY)
            .orElseThrow();

        Assertions.assertThat(previewLink.path()).startsWith("/app/verify-email?purpose=signup&token=");

        mockMvc.perform(get("/api/recovery-email/preview")
                .param("purpose", "signup")
                .param("token", previewLink.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maskedTargetEmail").isNotEmpty())
            .andExpect(jsonPath("$.alreadyConsumed").value(false))
            .andExpect(jsonPath("$.invalid").value(false));

        Assertions.assertThat(
            recoveryEmailVerificationTokenRepository
                .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                    savedMember.getId(),
                    RecoveryEmailVerificationPurpose.SIGNUP_VERIFY
                )
        ).isPresent();

        mockMvc.perform(post("/api/recovery-email/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "purpose": "signup",
                      "token": "%s"
                    }
                    """.formatted(previewLink.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.message").value("복구 이메일 인증이 완료되었습니다."));

        Member verifiedMember = memberRepository.findByUsername("verify_user")
            .orElseThrow();
        Assertions.assertThat(verifiedMember.getRecoveryEmail()).isEqualTo("verify@example.com");
        Assertions.assertThat(verifiedMember.getRecoveryEmailVerifiedAt()).isNotNull();
    }

    @Test
    void pendingRecoveryEmailDoesNotReserveVerifiedAddressAndResendInvalidatesPreviousToken() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "pending_user_a",
                      "password": "pending-password-a",
                      "displayName": "대기A",
                      "recoveryEmail": "shared@example.com"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "pending_user_b",
                      "password": "pending-password-b",
                      "displayName": "대기B",
                      "recoveryEmail": "shared@example.com"
                    }
                    """))
            .andExpect(status().isCreated());

        Member memberA = memberRepository.findByUsername("pending_user_a").orElseThrow();
        Member memberB = memberRepository.findByUsername("pending_user_b").orElseThrow();

        Assertions.assertThat(memberA.getRecoveryEmail()).isNull();
        Assertions.assertThat(memberB.getRecoveryEmail()).isNull();

        Long firstTokenId = recoveryEmailVerificationTokenRepository
            .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                memberA.getId(),
                RecoveryEmailVerificationPurpose.SIGNUP_VERIFY
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/settings/recovery-email/resend")
                .with(user("pending_user_a"))
                .with(csrf()))
            .andExpect(status().isNoContent());

        Assertions.assertThat(recoveryEmailVerificationTokenRepository.findById(firstTokenId))
            .get()
            .extracting(token -> token.getInvalidatedAt())
            .isNotNull();
        Assertions.assertThat(
            recoveryEmailVerificationTokenRepository
                .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                    memberA.getId(),
                    RecoveryEmailVerificationPurpose.SIGNUP_VERIFY
                )
        )
            .get()
            .extracting(token -> token.getId())
            .isNotEqualTo(firstTokenId);

        mockMvc.perform(get("/settings").with(user("pending_user_a")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/settings"));

        mockMvc.perform(get("/api/auth/session").with(user("pending_user_a")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recoveryEmailStatus.developmentPreviewPath").isNotEmpty())
            .andExpect(jsonPath("$.recoveryEmailStatus.hasPendingVerification").value(true));
    }

    @Test
    void recoveryEmailChangeKeepsExistingVerifiedAddressUntilConfirmed() throws Exception {
        Member member = createVerifiedMember("change_user", "change-password", "old@example.com");

        mockMvc.perform(post("/api/settings/recovery-email")
                .with(user("change_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newRecoveryEmail": "new@example.com",
                      "currentPassword": "change-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        Member unchangedMember = memberRepository.findById(member.getId()).orElseThrow();
        Assertions.assertThat(unchangedMember.getRecoveryEmail()).isEqualTo("old@example.com");
        Assertions.assertThat(unchangedMember.getRecoveryEmailVerifiedAt()).isNotNull();

        RecoveryEmailVerificationPreviewLink previewLink = recoveryEmailVerificationPreviewStore
            .find(member.getId(), RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE)
            .orElseThrow();

        Assertions.assertThat(previewLink.path()).startsWith("/app/verify-email?purpose=change&token=");
        Assertions.assertThat(
            recoveryEmailVerificationTokenRepository
                .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                    member.getId(),
                    RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE
                )
        )
            .get()
            .extracting(token -> token.getTargetEmail())
            .isEqualTo("new@example.com");
    }

    @Test
    void recoveryEmailChangeVerificationRequiresCurrentMemberAndPromotesNewAddress() throws Exception {
        Member member = createVerifiedMember("change_verify_user", "change-password", "before@example.com");
        createMember("different_user");

        mockMvc.perform(post("/api/settings/recovery-email")
                .with(user("change_verify_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newRecoveryEmail": "after@example.com",
                      "currentPassword": "change-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        RecoveryEmailVerificationPreviewLink previewLink = recoveryEmailVerificationPreviewStore
            .find(member.getId(), RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE)
            .orElseThrow();

        mockMvc.perform(get("/api/recovery-email/preview")
                .with(user("different_user"))
                .param("purpose", "change")
                .param("token", previewLink.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invalid").value(true));

        mockMvc.perform(get("/api/recovery-email/preview")
                .with(user("change_verify_user"))
                .param("purpose", "change")
                .param("token", previewLink.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maskedTargetEmail").isNotEmpty())
            .andExpect(jsonPath("$.alreadyConsumed").value(false))
            .andExpect(jsonPath("$.invalid").value(false));

        mockMvc.perform(post("/api/recovery-email/confirm")
                .with(user("change_verify_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "purpose": "change",
                      "token": "%s"
                    }
                    """.formatted(previewLink.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.message").value("복구 이메일 변경이 완료되었습니다."));

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        Assertions.assertThat(updatedMember.getRecoveryEmail()).isEqualTo("after@example.com");
        Assertions.assertThat(updatedMember.getRecoveryEmailVerifiedAt()).isNotNull();
    }

    @Test
    void requestingRecoveryEmailChangeInvalidatesOlderSignupVerificationLink() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "cross_purpose_user",
                      "password": "cross-purpose-password",
                      "displayName": "교차 목적 사용자",
                      "recoveryEmail": "signup@example.com"
                    }
                    """))
            .andExpect(status().isCreated());

        Member member = memberRepository.findByUsername("cross_purpose_user").orElseThrow();
        RecoveryEmailVerificationPreviewLink signupPreviewLink = recoveryEmailVerificationPreviewStore
            .find(member.getId(), RecoveryEmailVerificationPurpose.SIGNUP_VERIFY)
            .orElseThrow();
        Long signupTokenId = recoveryEmailVerificationTokenRepository
            .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                member.getId(),
                RecoveryEmailVerificationPurpose.SIGNUP_VERIFY
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/settings/recovery-email")
                .with(user("cross_purpose_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newRecoveryEmail": "change@example.com",
                      "currentPassword": "cross-purpose-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        Assertions.assertThat(recoveryEmailVerificationTokenRepository.findById(signupTokenId))
            .get()
            .extracting(token -> token.getInvalidatedAt())
            .isNotNull();

        mockMvc.perform(post("/api/recovery-email/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "purpose": "signup",
                      "token": "%s"
                    }
                    """.formatted(signupPreviewLink.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(false))
            .andExpect(jsonPath("$.message").value("유효하지 않거나 만료된 인증 링크입니다."));

        RecoveryEmailVerificationPreviewLink changePreviewLink = recoveryEmailVerificationPreviewStore
            .find(member.getId(), RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE)
            .orElseThrow();

        mockMvc.perform(post("/api/recovery-email/confirm")
                .with(user("cross_purpose_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "purpose": "change",
                      "token": "%s"
                    }
                    """.formatted(changePreviewLink.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.message").value("복구 이메일 변경이 완료되었습니다."));

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        Assertions.assertThat(updatedMember.getRecoveryEmail()).isEqualTo("change@example.com");
        Assertions.assertThat(updatedMember.getRecoveryEmailVerifiedAt()).isNotNull();
    }

    @Test
    void recoveryEmailResendUsesLatestPendingPurposeAndAppliesCooldown() throws Exception {
        Member member = createVerifiedMember("change_resend_user", "change-password", "verified@example.com");

        mockMvc.perform(post("/api/settings/recovery-email")
                .with(user("change_resend_user"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newRecoveryEmail": "pending-change@example.com",
                      "currentPassword": "change-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        Long firstTokenId = recoveryEmailVerificationTokenRepository
            .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                member.getId(),
                RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/settings/recovery-email/resend")
                .with(user("change_resend_user"))
                .with(csrf()))
            .andExpect(status().isNoContent());

        Assertions.assertThat(recoveryEmailVerificationTokenRepository.findById(firstTokenId))
            .get()
            .extracting(token -> token.getInvalidatedAt())
            .isNotNull();
        Assertions.assertThat(
            recoveryEmailVerificationTokenRepository
                .findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
                    member.getId(),
                    RecoveryEmailVerificationPurpose.RECOVERY_EMAIL_CHANGE
                )
        )
            .get()
            .extracting(token -> token.getId())
            .isNotEqualTo(firstTokenId);

        mockMvc.perform(post("/api/settings/recovery-email/resend")
                .with(user("change_resend_user"))
                .with(csrf()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("복구 이메일 재발송은 1분 뒤에 다시 시도할 수 있습니다."));
    }

    @Test
    void apiRecoveryEmailResendReturnsBadRequestWhenNoPendingTargetExists() throws Exception {
        mockMvc.perform(post("/api/settings/recovery-email/resend")
                .with(user("d"))
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("현재 재발송할 복구 이메일 인증 대상이 없습니다."));
    }

    @Test
    void settingsProfileUpdatePersistsDisplayNameAndRefreshesCurrentSession() throws Exception {
        Member member = createMember("set_profile_user", "profile-password", "이전 이름");

        mockMvc.perform(patch("/api/settings/profile")
                .with(user(BootSyncPrincipal.from(member)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName": "  새 이름  "
                    }
                    """))
            .andExpect(status().isNoContent());

        Assertions.assertThat(memberRepository.findByUsername("set_profile_user"))
            .get()
            .extracting(Member::getDisplayName)
            .isEqualTo("새 이름");

        mockMvc.perform(get("/api/auth/session")
                .with(user(BootSyncPrincipal.from(memberRepository.findByUsername("set_profile_user").orElseThrow()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.displayName").value("새 이름"));

        mockMvc.perform(get("/settings")
                .with(user(BootSyncPrincipal.from(memberRepository.findByUsername("set_profile_user").orElseThrow()))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/settings"));
    }

    @Test
    void settingsProfileUpdateRejectsTooShortTrimmedDisplayName() throws Exception {
        Member member = createMember("set_profile_short", "profile-password", "기존 이름");

        mockMvc.perform(patch("/api/settings/profile")
                .with(user("set_profile_short"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName": " a "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("표시 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다."));

        Assertions.assertThat(memberRepository.findById(member.getId()))
            .get()
            .extracting(Member::getDisplayName)
            .isEqualTo("기존 이름");
    }

    @Test
    void settingsPasswordChangeUpdatesPasswordAndAllowsNewLogin() throws Exception {
        Member member = createMember("set_password_user", "before-password", "비밀번호 사용자");

        mockMvc.perform(post("/api/settings/password")
                .with(user(BootSyncPrincipal.from(member)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "before-password",
                      "newPassword": "after-password",
                      "newPasswordConfirm": "after-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        Member updatedMember = memberRepository.findByUsername("set_password_user").orElseThrow();
        Assertions.assertThat(passwordEncoder.matches("after-password", updatedMember.getPasswordHash())).isTrue();
        Assertions.assertThat(passwordEncoder.matches("before-password", updatedMember.getPasswordHash())).isFalse();

        mockMvc.perform(get("/dashboard")
                .with(user(BootSyncPrincipal.from(updatedMember))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "set_password_user")
                .param("password", "before-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?error"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "set_password_user")
                .param("password", "after-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));
    }

    @Test
    void settingsPasswordChangeRejectsWrongCurrentPassword() throws Exception {
        Member member = createMember("set_password_bad", "before-password", "비밀번호 사용자");

        mockMvc.perform(post("/api/settings/password")
                .with(user("set_password_bad"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "not-the-current-password",
                      "newPassword": "after-password",
                      "newPasswordConfirm": "after-password"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("현재 비밀번호가 일치하지 않습니다."));

        Assertions.assertThat(memberRepository.findById(member.getId()))
            .get()
            .extracting(savedMember -> passwordEncoder.matches("before-password", savedMember.getPasswordHash()))
            .isEqualTo(true);
    }

    @Test
    void accountDeletionRequestRequiresVerifiedRecoveryEmail() throws Exception {
        Member member = createMember("delete_need_mail", "delete-password", "삭제 사용자");

        mockMvc.perform(post("/api/settings/account-deletion")
                .with(user("delete_need_mail"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "delete-password"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("검증된 복구 이메일이 있는 계정만 삭제 요청을 접수할 수 있습니다."));

        Assertions.assertThat(memberRepository.findById(member.getId()))
            .get()
            .extracting(Member::getStatus)
            .isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void accountDeletionRequestMarksMemberPendingDeleteAndLogsOutCurrentSession() throws Exception {
        Member member = createVerifiedMember("delete_req_user", "delete-password", "delete@example.com");

        mockMvc.perform(post("/api/settings/account-deletion")
                .with(user(BootSyncPrincipal.from(member)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "delete-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        Member pendingMember = memberRepository.findById(member.getId()).orElseThrow();
        Assertions.assertThat(pendingMember.getStatus()).isEqualTo(MemberStatus.PENDING_DELETE);
        Assertions.assertThat(pendingMember.getDeleteRequestedAt()).isNotNull();
        Assertions.assertThat(pendingMember.getDeleteDueAt()).isEqualTo(pendingMember.getDeleteRequestedAt().plusDays(7));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "delete_req_user")
                .param("password", "delete-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?error"));
    }

    @Test
    void accountDeletionCancelRestoresActiveStatusAndRequiresFreshLogin() throws Exception {
        Member member = createVerifiedMember("delete_cancel", "delete-password", "delete-cancel@example.com");

        mockMvc.perform(post("/api/settings/account-deletion")
                .with(user(BootSyncPrincipal.from(member)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentPassword": "delete-password"
                    }
                    """))
            .andExpect(status().isNoContent());

        accountDeletionService.cancelDeletion("delete_cancel");

        Member restoredMember = memberRepository.findById(member.getId()).orElseThrow();
        Assertions.assertThat(restoredMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        Assertions.assertThat(restoredMember.getDeleteRequestedAt()).isNull();
        Assertions.assertThat(restoredMember.getDeleteDueAt()).isNull();

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "delete_cancel")
                .param("password", "delete-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));
    }

    @Test
    void operatorPasswordResetInvalidatesExistingSessionAndAllowsTemporaryPasswordLogin() throws Exception {
        createVerifiedMember("ops_reset_user", "before-password", "ops-reset@example.com");
        MockHttpSession session = loginSession("ops_reset_user", "before-password");

        operatorPasswordResetService.resetPassword("ops_reset_user", "after-password-123");

        mockMvc.perform(authenticatedRequest(get("/dashboard"), session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?reason=session_expired"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "ops_reset_user")
                .param("password", "before-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?error"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "ops_reset_user")
                .param("password", "after-password-123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/dashboard"));
    }

    @Test
    void operatorPasswordResetRequiresVerifiedRecoveryEmail() {
        createMember("ops_need_mail", "before-password", "복구 없음");

        Assertions.assertThatThrownBy(() -> operatorPasswordResetService.resetPassword("ops_need_mail", "after-password-123"))
            .isInstanceOf(com.bootsync.member.service.MemberValidationException.class)
            .hasMessage("검증된 복구 이메일이 있는 계정만 운영자 보조 비밀번호 초기화를 진행할 수 있습니다.");
    }

    @Test
    void operatorPasswordResetRejectsPendingDeleteAccount() {
        Member member = createVerifiedMember("ops_pending_reset", "before-password", "ops-pending@example.com");
        member.setStatus(MemberStatus.PENDING_DELETE);
        member.setDeleteRequestedAt(LocalDateTime.now(clock).minusHours(1));
        member.setDeleteDueAt(LocalDateTime.now(clock).plusDays(6));
        memberRepository.save(member);

        Assertions.assertThatThrownBy(() -> operatorPasswordResetService.resetPassword("ops_pending_reset", "after-password-123"))
            .isInstanceOf(com.bootsync.member.service.MemberValidationException.class)
            .hasMessage("ACTIVE 상태 계정만 운영자 보조 비밀번호 초기화를 진행할 수 있습니다.");
    }

    @Test
    void signupRateLimitBlocksAfterFiveRequestsPerIp() throws Exception {
        for (int index = 1; index <= 5; index++) {
            mockMvc.perform(post("/api/auth/signup")
                    .with(csrf())
                    .header("X-Forwarded-For", "203.0.113.10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "username": "rate_signup_%s",
                          "password": "signup-password-%s",
                          "displayName": "가입%s",
                          "recoveryEmail": "signup-rate-%s@example.com"
                        }
                        """.formatted(index, index, index, index)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/auth/signup")
                .with(csrf())
                .header("X-Forwarded-For", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "rate_signup_6",
                      "password": "signup-password-6",
                      "displayName": "가입6",
                      "recoveryEmail": "signup-rate-6@example.com"
                    }
                    """))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("회원가입 요청이 너무 많습니다. 1시간 후 다시 시도해 주세요."));
    }

    @Test
    void loginRateLimitBlocksAfterRepeatedFailuresForSameUsername() throws Exception {
        for (int index = 1; index <= 5; index++) {
            mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .param("username", "d")
                    .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/login?error"));
        }

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "d")
                .param("password", "wrong-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?rateLimit"));
    }

    @Test
    void loginRateLimitBlocksAfterTenRequestsFromSameIp() throws Exception {
        for (int index = 1; index <= 10; index++) {
            mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .header("X-Forwarded-For", "198.51.100.20")
                    .param("username", "")
                    .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/login?error"));
        }

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .header("X-Forwarded-For", "198.51.100.20")
                .param("username", "")
                .param("password", "wrong-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/login?rateLimit"));
    }

    @Test
    void snippetCreatePersistsForCurrentMemberWithTags() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();

        mockMvc.perform(post("/api/snippets")
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "새 스니펫 작성 테스트",
                      "content": "새 스니펫 본문",
                      "tags": ["Spring", "tips", "spring"]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("새 스니펫 작성 테스트"));

        Snippet savedSnippet = snippetRepository.findFirstByMemberIdAndTitle(demoMemberId, "새 스니펫 작성 테스트")
            .orElseThrow();

        Assertions.assertThat(savedSnippet.getContentMarkdown()).isEqualTo("새 스니펫 본문");
        Assertions.assertThat(tagRepository.findNamesBySnippetIdAndMemberId(savedSnippet.getId(), demoMemberId))
            .containsExactly("spring", "tips");
    }

    @Test
    void snippetSecretWarningBlocksFirstCreateAndAllowsAcknowledgedRetry() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        MockHttpSession session = new MockHttpSession();

        String riskyContent = "AWS key sample: AKIA1234567890ABCD12";

        String warningResponse = mockMvc.perform(post("/api/snippets")
                .session(session)
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "시크릿 경고 테스트",
                      "content": "%s",
                      "tags": ["aws"]
                    }
                    """.formatted(riskyContent)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("민감 정보로 보이는 내용이 감지되었습니다. 경고를 확인한 뒤 같은 내용으로 다시 제출해야 저장할 수 있습니다."))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Assertions.assertThat(snippetRepository.findFirstByMemberIdAndTitle(demoMemberId, "시크릿 경고 테스트")).isEmpty();

        String secretWarningToken = extractJsonStringValue(warningResponse, "secretWarningToken");

        mockMvc.perform(post("/api/snippets")
                .session(session)
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "시크릿 경고 테스트",
                      "content": "%s",
                      "tags": ["aws"],
                      "secretWarningToken": "%s"
                    }
                    """.formatted(riskyContent, secretWarningToken)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("시크릿 경고 테스트"));

        Assertions.assertThat(snippetRepository.findFirstByMemberIdAndTitle(demoMemberId, "시크릿 경고 테스트")).isPresent();
    }

    @Test
    void snippetSecretWarningRejectsChangedContentDuringUpdate() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Snippet snippet = snippetRepository.save(createSnippet(demoMemberId, "시크릿 수정 대상", "safe content"));
        MockHttpSession session = new MockHttpSession();

        String firstRiskyContent = "JWT sample: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib290c3luYyJ9.signature";
        String changedRiskyContent = firstRiskyContent + " changed";
        String warningResponse = mockMvc.perform(put("/api/snippets/" + snippet.getId())
                .session(session)
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "시크릿 수정 대상",
                      "content": "%s",
                      "tags": ["jwt"]
                    }
                    """.formatted(firstRiskyContent)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("민감 정보로 보이는 내용이 감지되었습니다. 경고를 확인한 뒤 같은 내용으로 다시 제출해야 저장할 수 있습니다."))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String secretWarningToken = extractJsonStringValue(warningResponse, "secretWarningToken");

        mockMvc.perform(put("/api/snippets/" + snippet.getId())
                .session(session)
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "시크릿 수정 대상",
                      "content": "%s",
                      "tags": ["jwt"],
                      "secretWarningToken": "%s"
                    }
                    """.formatted(changedRiskyContent, secretWarningToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("민감 정보로 보이는 내용이 감지되었습니다. 경고를 확인한 뒤 같은 내용으로 다시 제출해야 저장할 수 있습니다."));

        Assertions.assertThat(snippetRepository.findByIdAndMemberId(snippet.getId(), demoMemberId))
            .get()
            .extracting(Snippet::getContentMarkdown)
            .isEqualTo("safe content");
    }

    @Test
    void snippetUpdateReplacesContentAndTagsForCurrentMember() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Tag oldTag = createTag(demoMemberId, "legacy");
        Snippet snippet = snippetRepository.save(createSnippet(demoMemberId, "수정 대상 스니펫", "before content"));
        snippetTagRepository.save(link(demoMemberId, snippet.getId(), oldTag.getId()));

        mockMvc.perform(put("/api/snippets/" + snippet.getId())
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "수정 완료 스니펫",
                      "content": "after content",
                      "tags": ["updated", "spring"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("수정 완료 스니펫"));

        Snippet updatedSnippet = snippetRepository.findByIdAndMemberId(snippet.getId(), demoMemberId)
            .orElseThrow();

        Assertions.assertThat(updatedSnippet.getTitle()).isEqualTo("수정 완료 스니펫");
        Assertions.assertThat(updatedSnippet.getContentMarkdown()).isEqualTo("after content");
        Assertions.assertThat(tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), demoMemberId))
            .containsExactly("spring", "updated");
        Assertions.assertThat(tagRepository.findByMemberIdAndNormalizedName(demoMemberId, "legacy")).isEmpty();
    }

    @Test
    void snippetEditIsNotAvailableForOtherMembersSnippet() throws Exception {
        Member otherMember = createMember("snippet_editor");
        Snippet otherSnippet = snippetRepository.save(createSnippet(otherMember.getId(), "다른 사용자 스니펫", "secret"));

        mockMvc.perform(get("/snippets/" + otherSnippet.getId() + "/edit").with(user("d")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/snippets/" + otherSnippet.getId() + "/edit"));

        mockMvc.perform(put("/api/snippets/" + otherSnippet.getId())
                .with(user("d"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "변경 시도",
                      "content": "blocked",
                      "tags": ["blocked"]
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void snippetDeleteRemovesCurrentMembersSnippetAndUnusedTags() throws Exception {
        Long demoMemberId = memberRepository.findByUsername("d")
            .orElseThrow()
            .getId();
        Tag deleteOnlyTag = createTag(demoMemberId, "delete-only");
        Snippet snippet = snippetRepository.save(createSnippet(demoMemberId, "삭제 대상 스니펫", "delete me"));
        snippetTagRepository.save(link(demoMemberId, snippet.getId(), deleteOnlyTag.getId()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/snippets/" + snippet.getId())
                .with(user("d"))
                .with(csrf()))
            .andExpect(status().isNoContent());

        Assertions.assertThat(snippetRepository.findByIdAndMemberId(snippet.getId(), demoMemberId)).isEmpty();
        Assertions.assertThat(tagRepository.findByMemberIdAndNormalizedName(demoMemberId, "delete-only")).isEmpty();

        mockMvc.perform(get("/api/snippets").with(user("d")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("delete-only"))));
    }

    @Test
    void snippetDeleteReturns404ForOtherMembersSnippet() throws Exception {
        Member otherMember = createMember("snippet_delete_other");
        Snippet otherSnippet = snippetRepository.save(createSnippet(otherMember.getId(), "다른 사용자 삭제 대상", "secret"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/snippets/" + otherSnippet.getId())
                .with(user("d"))
                .with(csrf()))
            .andExpect(status().isNotFound());

        Assertions.assertThat(snippetRepository.findByIdAndMemberId(otherSnippet.getId(), otherMember.getId())).isPresent();
    }

    private Member createMember(String username) {
        return createMember(username, username + "-password");
    }

    private Member createMember(String username, String rawPassword) {
        return createMember(username, rawPassword, username);
    }

    private Member createMember(String username, String rawPassword, String displayName) {
        Member member = new Member();
        member.setUsername(username);
        member.setPasswordHash(passwordEncoder.encode(rawPassword));
        member.setDisplayName(displayName);
        member.setCreatedAt(LocalDateTime.now(clock));
        member.setUpdatedAt(LocalDateTime.now(clock));
        return memberRepository.save(member);
    }

    private Member createVerifiedMember(String username, String rawPassword, String recoveryEmail) {
        Member member = createMember(username, rawPassword);
        member.setRecoveryEmail(recoveryEmail);
        member.setRecoveryEmailVerifiedAt(LocalDateTime.now(clock));
        member.setUpdatedAt(LocalDateTime.now(clock));
        return memberRepository.save(member);
    }

    private Snippet createSnippet(Long memberId, String title, String contentMarkdown) {
        Snippet snippet = new Snippet();
        snippet.setMemberId(memberId);
        snippet.setTitle(title);
        snippet.setContentMarkdown(contentMarkdown);
        snippet.setCreatedAt(LocalDateTime.now(clock));
        snippet.setUpdatedAt(LocalDateTime.now(clock));
        return snippet;
    }

    private Tag createTag(Long memberId, String name) {
        Tag tag = new Tag();
        tag.setMemberId(memberId);
        tag.setName(name);
        tag.setNormalizedName(name);
        return tagRepository.save(tag);
    }

    private AttendanceRecord createAttendanceRecord(Long memberId, LocalDate attendanceDate, AttendanceStatus status, String memo) {
        AttendanceRecord record = new AttendanceRecord();
        record.setMemberId(memberId);
        record.setAttendanceDate(attendanceDate);
        record.setStatus(status);
        record.setMemo(memo);
        record.setCreatedAt(LocalDateTime.now(clock));
        record.setUpdatedAt(LocalDateTime.now(clock));
        return record;
    }

    private LocalDate firstMatchingDate(
        LocalDate startDate,
        LocalDate endDate,
        Set<DayOfWeek> trainingDays,
        Set<LocalDate> excludedDates
    ) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (trainingDays.contains(date.getDayOfWeek()) && !excludedDates.contains(date)) {
                return date;
            }
        }
        throw new IllegalStateException("조건에 맞는 날짜를 찾지 못했습니다.");
    }

    private int countEligibleTrainingDays(
        LocalDate startDate,
        LocalDate endDate,
        Set<DayOfWeek> trainingDays,
        Set<LocalDate> holidayDates,
        Set<LocalDate> existingDates
    ) {
        int count = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!trainingDays.contains(date.getDayOfWeek())) {
                continue;
            }
            if (holidayDates.contains(date) || existingDates.contains(date)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private SnippetTag link(Long memberId, Long snippetId, Long tagId) {
        SnippetTag snippetTag = new SnippetTag();
        snippetTag.setId(new SnippetTagId(memberId, snippetId, tagId));
        return snippetTag;
    }

    private String extractJsonStringValue(String json, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"")
            .matcher(json);
        Assertions.assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private MockHttpSession loginSession(String username, String password) throws Exception {
        return copySession((MockHttpSession) mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", username)
                .param("password", password))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getRequest()
            .getSession(false));
    }

    private MockHttpSession copySession(MockHttpSession session) {
        MockHttpSession copiedSession = new MockHttpSession();
        if (session == null) {
            return copiedSession;
        }

        Object securityContext = session.getAttribute("SPRING_SECURITY_CONTEXT");
        if (securityContext != null) {
            copiedSession.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        }

        Object activeMemberMarker = session.getAttribute(ActiveMemberSessionFilter.ACTIVE_MEMBER_REVALIDATION_MARKER);
        if (activeMemberMarker != null) {
            copiedSession.setAttribute(ActiveMemberSessionFilter.ACTIVE_MEMBER_REVALIDATION_MARKER, activeMemberMarker);
        }
        return copiedSession;
    }

    private MockHttpServletRequestBuilder authenticatedRequest(MockHttpServletRequestBuilder builder, MockHttpSession session) {
        return builder
            .session(session)
            .sessionAttr("SPRING_SECURITY_CONTEXT", session.getAttribute("SPRING_SECURITY_CONTEXT"))
            .sessionAttr(
                ActiveMemberSessionFilter.ACTIVE_MEMBER_REVALIDATION_MARKER,
                session.getAttribute(ActiveMemberSessionFilter.ACTIVE_MEMBER_REVALIDATION_MARKER)
            );
    }

    private boolean frontendShellAvailable() {
        return new ClassPathResource("static/app/index.html").exists();
    }
}
