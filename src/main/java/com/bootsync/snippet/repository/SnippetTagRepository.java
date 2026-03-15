package com.bootsync.snippet.repository;

import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnippetTagRepository extends JpaRepository<SnippetTag, SnippetTagId> {

    long countByIdMemberId(Long memberId);

    void deleteByIdMemberId(Long memberId);

    void deleteByIdMemberIdAndIdSnippetId(Long memberId, Long snippetId);
}
