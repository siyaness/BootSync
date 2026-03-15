package com.bootsync.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsync.attendance.entity.AttendanceAuditLog;
import com.bootsync.attendance.repository.AttendanceAuditLogRepository;
import com.bootsync.config.RequestIpHmacService;
import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.audit.request-ip-hmac-secret=test-audit-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceAuditLogRequestIpHmacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

    @Autowired
    private RequestIpHmacService requestIpHmacService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void apiAttendanceUpsertStoresRequestIpHmacWhenSecretIsConfigured() throws Exception {
        Member member = createMember("audit_ip_user", "audit-ip-password");
        LocalDate targetDate = LocalDate.now(clock).minusDays(1);
        String clientIp = "198.51.100.10";

        mockMvc.perform(put("/api/attendance/" + targetDate)
                .with(user(member.getUsername()))
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "출석",
                      "memo": "audit memo"
                    }
                    """))
            .andExpect(status().isOk());

        AttendanceAuditLog auditLog = attendanceAuditLogRepository.findAll().stream()
            .filter(log -> member.getId().equals(log.getMemberId()))
            .findFirst()
            .orElseThrow();

        Assertions.assertThat(auditLog.getRequestIpHmac()).isEqualTo(requestIpHmacService.hmac(clientIp));
    }

    private Member createMember(String username, String rawPassword) {
        Member member = new Member();
        member.setUsername(username);
        member.setPasswordHash(passwordEncoder.encode(rawPassword));
        member.setDisplayName(username);
        member.setCreatedAt(LocalDateTime.now(clock));
        member.setUpdatedAt(LocalDateTime.now(clock));
        return memberRepository.save(member);
    }
}
