package com.bootsync.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.member.service.RecoveryEmailVerificationService;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.recovery-email.development-preview-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecoveryEmailPreviewExposureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RecoveryEmailVerificationService recoveryEmailVerificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void apiSessionOmitsDevelopmentPreviewPathWhenPreviewModeIsDisabled() throws Exception {
        Member member = createMember("preview_hidden_user", "preview-password");
        recoveryEmailVerificationService.issueSignupVerification(member, "preview-hidden@example.com");

        mockMvc.perform(get("/api/auth/session").with(user(member.getUsername())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.recoveryEmailStatus.hasPendingVerification").value(true))
            .andExpect(jsonPath("$.recoveryEmailStatus.developmentPreviewPath").value(nullValue()));
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
