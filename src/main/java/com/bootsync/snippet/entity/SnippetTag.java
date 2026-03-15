package com.bootsync.snippet.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "snippet_tag")
public class SnippetTag {

    @EmbeddedId
    private SnippetTagId id;

    public SnippetTagId getId() {
        return id;
    }

    public void setId(SnippetTagId id) {
        this.id = id;
    }
}
