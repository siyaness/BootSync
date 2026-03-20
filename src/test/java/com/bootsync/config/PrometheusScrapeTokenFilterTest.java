package com.bootsync.config;

import com.bootsync.common.time.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PrometheusScrapeTokenFilterTest {

    @Test
    void doFilterRejectsBlankConfiguredToken() throws Exception {
        AppProperties appProperties = new AppProperties();
        appProperties.getMonitoring().setPrometheusScrapeToken("");

        PrometheusScrapeTokenFilter filter = new PrometheusScrapeTokenFilter(appProperties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        Assertions.assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        Assertions.assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer realm=\"bootsync-prometheus\"");
        Assertions.assertThat(filterChain.getRequest()).isNull();
    }
}
