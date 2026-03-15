package com.bootsync.config;

import com.bootsync.common.time.AppProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestIpHmacService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final AppProperties appProperties;

    public RequestIpHmacService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String hmac(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return null;
        }

        String secret = appProperties.getAudit().getRequestIpHmacSecret();
        if (!StringUtils.hasText(secret)) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(clientIp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("request_ip_hmac 계산에 실패했습니다.", exception);
        }
    }
}
