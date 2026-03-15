package com.bootsync.tag.repository;

import com.bootsync.tag.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    long countByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    List<Tag> findByMemberIdOrderByNameAsc(Long memberId);

    Optional<Tag> findByMemberIdAndNormalizedName(Long memberId, String normalizedName);

    @Query(
        value = """
            select distinct t.*
            from tag t
            join snippet_tag st
              on st.member_id = t.member_id
             and st.tag_id = t.id
            where t.member_id = :memberId
            order by t.name asc
            """,
        nativeQuery = true
    )
    List<Tag> findUsedByMemberIdOrderByNameAsc(@Param("memberId") Long memberId);

    @Query(
        value = """
            select t.name
            from tag t
            join snippet_tag st
              on st.member_id = t.member_id
             and st.tag_id = t.id
            where st.member_id = :memberId
              and st.snippet_id = :snippetId
            order by t.name asc
            """,
        nativeQuery = true
    )
    List<String> findNamesBySnippetIdAndMemberId(@Param("snippetId") Long snippetId, @Param("memberId") Long memberId);

    @Query(
        """
            select t.id
            from Tag t
            where t.memberId = :memberId
              and not exists (
                    select 1
                    from SnippetTag st
                    where st.id.memberId = t.memberId
                      and st.id.tagId = t.id
              )
            """
    )
    List<Long> findOrphanedIdsByMemberId(@Param("memberId") Long memberId);
}
