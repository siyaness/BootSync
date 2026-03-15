package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@ConditionalOnProperty(name = "app.account-deletion.purge-run-once.enabled", havingValue = "true")
public class AccountDeletionPurgeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionPurgeRunner.class);

    private final AppProperties appProperties;
    private final ConfigurableApplicationContext applicationContext;
    private final AccountDeletionPurgeService accountDeletionPurgeService;

    public AccountDeletionPurgeRunner(
        AppProperties appProperties,
        ConfigurableApplicationContext applicationContext,
        AccountDeletionPurgeService accountDeletionPurgeService
    ) {
        this.appProperties = appProperties;
        this.applicationContext = applicationContext;
        this.accountDeletionPurgeService = accountDeletionPurgeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.PurgeRunOnce purgeRunOnce = appProperties.getAccountDeletion().getPurgeRunOnce();
        String actor = requireText(purgeRunOnce.getActor(), "app.account-deletion.purge-run-once.actor");
        String reason = requireText(purgeRunOnce.getReason(), "app.account-deletion.purge-run-once.reason");

        int purgedCount = accountDeletionPurgeService.purgeDueMembers();
        log.info(
            "BootSync one-shot account deletion purge completed: purgedCount={}, actor={}, reason={}",
            purgedCount,
            actor,
            reason
        );

        if (purgeRunOnce.isCloseContextAfterRun()) {
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalStateException("필수 운영 설정이 비어 있습니다: " + fieldName);
        }
        return value.trim();
    }
}
