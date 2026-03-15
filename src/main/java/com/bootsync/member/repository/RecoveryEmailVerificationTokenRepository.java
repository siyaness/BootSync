package com.bootsync.member.repository;

import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import com.bootsync.member.entity.RecoveryEmailVerificationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryEmailVerificationTokenRepository extends JpaRepository<RecoveryEmailVerificationToken, Long> {

    long countByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    Optional<RecoveryEmailVerificationToken> findByTokenHash(String tokenHash);

    List<RecoveryEmailVerificationToken> findByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNull(Long memberId);

    List<RecoveryEmailVerificationToken> findByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNull(
        Long memberId,
        RecoveryEmailVerificationPurpose purpose
    );

    Optional<RecoveryEmailVerificationToken> findTopByMemberIdAndPurposeAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(
        Long memberId,
        RecoveryEmailVerificationPurpose purpose
    );

    Optional<RecoveryEmailVerificationToken> findTopByMemberIdAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByIssuedAtDesc(Long memberId);
}
