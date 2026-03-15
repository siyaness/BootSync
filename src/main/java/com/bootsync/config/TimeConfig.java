package com.bootsync.config;

import com.bootsync.common.time.AppProperties;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    Clock appClock(AppProperties appProperties) {
        return Clock.system(ZoneId.of(appProperties.getTimezone()));
    }
}
