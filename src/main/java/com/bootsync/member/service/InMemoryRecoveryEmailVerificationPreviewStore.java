package com.bootsync.member.service;

import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class InMemoryRecoveryEmailVerificationPreviewStore implements RecoveryEmailVerificationPreviewStore {

    private final Map<String, RecoveryEmailVerificationPreviewLink> previews = new ConcurrentHashMap<>();

    @Override
    public void remember(Long memberId, RecoveryEmailVerificationPurpose purpose, RecoveryEmailVerificationPreviewLink previewLink) {
        previews.put(key(memberId, purpose), previewLink);
    }

    @Override
    public Optional<RecoveryEmailVerificationPreviewLink> find(Long memberId, RecoveryEmailVerificationPurpose purpose) {
        return Optional.ofNullable(previews.get(key(memberId, purpose)));
    }

    private String key(Long memberId, RecoveryEmailVerificationPurpose purpose) {
        return memberId + ":" + purpose.name();
    }
}
