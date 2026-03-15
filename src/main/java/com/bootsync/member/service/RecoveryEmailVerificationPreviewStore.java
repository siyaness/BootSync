package com.bootsync.member.service;

import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import java.util.Optional;

public interface RecoveryEmailVerificationPreviewStore {

    void remember(Long memberId, RecoveryEmailVerificationPurpose purpose, RecoveryEmailVerificationPreviewLink previewLink);

    Optional<RecoveryEmailVerificationPreviewLink> find(Long memberId, RecoveryEmailVerificationPurpose purpose);
}
