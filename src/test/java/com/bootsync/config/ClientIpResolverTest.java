package com.bootsync.config;

import com.bootsync.common.time.AppProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolveUsesRemoteAddrWhenForwardedHeadersAreNotTrusted() {
        AppProperties appProperties = new AppProperties();
        ClientIpResolver clientIpResolver = new ClientIpResolver(appProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.10");

        Assertions.assertThat(clientIpResolver.resolve(request)).isEqualTo("10.0.0.10");
    }

    @Test
    void resolveUsesForwardedHeaderWhenTrustIsEnabled() {
        AppProperties appProperties = new AppProperties();
        appProperties.getSecurity().setTrustForwardedHeaders(true);
        ClientIpResolver clientIpResolver = new ClientIpResolver(appProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.10");

        Assertions.assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }
}
