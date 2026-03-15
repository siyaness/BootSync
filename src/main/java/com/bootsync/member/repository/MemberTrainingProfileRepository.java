package com.bootsync.member.repository;

import com.bootsync.member.entity.MemberTrainingProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTrainingProfileRepository extends JpaRepository<MemberTrainingProfile, Long> {

    long countByMemberId(Long memberId);

    Optional<MemberTrainingProfile> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
