package com.bootsync.member.repository;

import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.MemberStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUsername(String username);

    boolean existsByRecoveryEmailIgnoreCase(String recoveryEmail);

    Optional<Member> findByRecoveryEmailIgnoreCase(String recoveryEmail);

    Optional<Member> findByUsername(String username);

    List<Member> findByStatusAndDeleteDueAtLessThanEqualOrderByDeleteDueAtAscIdAsc(MemberStatus status, LocalDateTime deleteDueAt);
}
