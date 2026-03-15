package com.bootsync.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "server.servlet.session.cookie.name=CUSTOMSYNCSESSION")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutCookieNameTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logoutClearsConfiguredSessionCookieName() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/logout")
                .with(user("d"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        Assertions.assertThat(result.getResponse().getHeaders("Set-Cookie"))
            .anyMatch(header -> header.contains("CUSTOMSYNCSESSION="));
    }
}
