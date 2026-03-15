package com.bootsync.member.service;

import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecoveryEmailVerificationPreviewStoreConfig {

    @Bean
    @ConditionalOnMissingBean(RecoveryEmailVerificationPreviewStore.class)
    RecoveryEmailVerificationPreviewStore recoveryEmailVerificationPreviewStore() {
        return new RecoveryEmailVerificationPreviewStore() {
            @Override
            public void remember(
                Long memberId,
                RecoveryEmailVerificationPurpose purpose,
                RecoveryEmailVerificationPreviewLink previewLink
            ) {
            }

            @Override
            public Optional<RecoveryEmailVerificationPreviewLink> find(Long memberId, RecoveryEmailVerificationPurpose purpose) {
                return Optional.empty();
            }
        };
    }
}
