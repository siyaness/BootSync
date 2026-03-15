package com.bootsync.snippet.repository;

import com.bootsync.snippet.entity.Snippet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    void deleteByMemberId(Long memberId);

    Optional<Snippet> findByIdAndMemberId(Long id, Long memberId);

    Optional<Snippet> findFirstByMemberIdAndTitle(Long memberId, String title);

    long countByMemberId(Long memberId);

    List<Snippet> findTop5ByMemberIdOrderByUpdatedAtDesc(Long memberId);

    @Query(
        value = """
            select distinct s.*
            from snippet s
            left join snippet_tag st
                on st.member_id = s.member_id
                and st.snippet_id = s.id
            left join tag t
                on t.member_id = st.member_id
                and t.id = st.tag_id
            where s.member_id = :memberId
              and (:query = '' or lower(s.title) like concat('%', :query, '%')
                   or lower(s.content_markdown) like concat('%', :query, '%'))
              and (:tag = '' or t.normalized_name = :tag)
            order by s.updated_at desc
            """,
        nativeQuery = true
    )
    List<Snippet> searchByMemberIdAndFilters(
        @Param("memberId") Long memberId,
        @Param("query") String query,
        @Param("tag") String tag
    );
}
