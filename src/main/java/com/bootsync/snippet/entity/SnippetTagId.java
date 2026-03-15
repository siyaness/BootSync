package com.bootsync.snippet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SnippetTagId implements Serializable {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "snippet_id", nullable = false)
    private Long snippetId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public SnippetTagId() {
    }

    public SnippetTagId(Long memberId, Long snippetId, Long tagId) {
        this.memberId = memberId;
        this.snippetId = snippetId;
        this.tagId = tagId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getSnippetId() {
        return snippetId;
    }

    public void setSnippetId(Long snippetId) {
        this.snippetId = snippetId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SnippetTagId that)) {
            return false;
        }
        return Objects.equals(memberId, that.memberId)
            && Objects.equals(snippetId, that.snippetId)
            && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, snippetId, tagId);
    }
}
