package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import com.bootsync.member.entity.Member;
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
@Order(31)
@ConditionalOnProperty(name = "app.operations.deletion-cancel.enabled", havingValue = "true")
public class AccountDeletionCancelRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionCancelRunner.class);

    private final AppProperties appProperties;
    private final ConfigurableApplicationContext applicationContext;
    private final AccountDeletionService accountDeletionService;

    public AccountDeletionCancelRunner(
        AppProperties appProperties,
        ConfigurableApplicationContext applicationContext,
        AccountDeletionService accountDeletionService
    ) {
        this.appProperties = appProperties;
        this.applicationContext = applicationContext;
        this.accountDeletionService = accountDeletionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.DeletionCancel deletionCancel = appProperties.getOperations().getDeletionCancel();
        String username = requireText(deletionCancel.getUsername(), "app.operations.deletion-cancel.username");
        String actor = requireText(deletionCancel.getActor(), "app.operations.deletion-cancel.actor");
        String reason = requireText(deletionCancel.getReason(), "app.operations.deletion-cancel.reason");

        Member restoredMember = accountDeletionService.cancelDeletion(username);
        log.info(
            "BootSync operator account deletion cancel completed: memberId={}, username={}, actor={}, reason={}",
            restoredMember.getId(),
            restoredMember.getUsername(),
            actor,
            reason
        );

        if (deletionCancel.isCloseContextAfterRun()) {
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
