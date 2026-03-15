package com.bootsync.member.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.account-deletion.purge-enabled", havingValue = "true")
public class AccountDeletionPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionPurgeJob.class);

    private final AccountDeletionPurgeService accountDeletionPurgeService;

    public AccountDeletionPurgeJob(AccountDeletionPurgeService accountDeletionPurgeService) {
        this.accountDeletionPurgeService = accountDeletionPurgeService;
    }

    @Scheduled(cron = "${app.account-deletion.purge-cron:0 15 3 * * *}", zone = "${app.timezone:Asia/Seoul}")
    public void purgeDueMembers() {
        int purgedCount = accountDeletionPurgeService.purgeDueMembers();
        log.info("BootSync scheduled account deletion purge completed: purgedCount={}", purgedCount);
    }
}
