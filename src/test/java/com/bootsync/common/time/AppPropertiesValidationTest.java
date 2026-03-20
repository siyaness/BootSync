package com.bootsync.common.time;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            ValidationAutoConfiguration.class
        ))
        .withUserConfiguration(AppPropertiesTestConfiguration.class)
        .withPropertyValues("app.monitoring.prometheus-scrape-token=test-prometheus-token");

    @Test
    void startupFailsWhenNestedMonitoringTokenIsBlank() {
        contextRunner
            .withPropertyValues("app.monitoring.prometheus-scrape-token=")
            .run(context -> {
                Assertions.assertThat(context).hasFailed();
                Assertions.assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("monitoring.prometheusScrapeToken");
            });
    }

    @Test
    void startupFailsWhenNestedAllowanceCapIsInvalid() {
        contextRunner
            .withPropertyValues("app.allowance.payable-day-cap=0")
            .run(context -> {
                Assertions.assertThat(context).hasFailed();
                Assertions.assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("allowance.payableDayCap");
            });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class AppPropertiesTestConfiguration {
    }
}
