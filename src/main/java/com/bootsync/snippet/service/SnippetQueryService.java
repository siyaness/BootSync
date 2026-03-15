package com.bootsync.snippet.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.snippet.dto.SnippetContentView;
import com.bootsync.snippet.dto.SnippetDetailView;
import com.bootsync.snippet.dto.SnippetSearchItemResponse;
import com.bootsync.snippet.dto.SnippetSearchRequest;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.tag.repository.TagRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SnippetQueryService {

    private final MemberRepository memberRepository;
    private final SnippetRepository snippetRepository;
    private final TagRepository tagRepository;
    private final SnippetMarkdownRenderer snippetMarkdownRenderer;

    public SnippetQueryService(
        MemberRepository memberRepository,
        SnippetRepository snippetRepository,
        TagRepository tagRepository,
        SnippetMarkdownRenderer snippetMarkdownRenderer
    ) {
        this.memberRepository = memberRepository;
        this.snippetRepository = snippetRepository;
        this.tagRepository = tagRepository;
        this.snippetMarkdownRenderer = snippetMarkdownRenderer;
    }

    public List<SnippetSearchItemResponse> searchForMember(String username, SnippetSearchRequest searchRequest) {
        Long memberId = memberIdFor(username);
        String normalizedQuery = normalize(searchRequest.q());
        String normalizedTag = normalize(searchRequest.tag());

        return snippetRepository.searchByMemberIdAndFilters(memberId, normalizedQuery, normalizedTag).stream()
            .map(snippet -> new SnippetSearchItemResponse(
                snippet.getId(),
                snippet.getTitle(),
                tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), memberId),
                snippet.getUpdatedAt(),
                buildHighlightText(snippet, normalizedQuery)
            ))
            .toList();
    }

    public List<String> availableTagsForMember(String username) {
        Long memberId = memberIdFor(username);
        return tagRepository.findUsedByMemberIdOrderByNameAsc(memberId).stream()
            .map(tag -> tag.getName())
            .toList();
    }

    public SnippetDetailView detailForMember(String username, Long snippetId) {
        Long memberId = memberIdFor(username);
        Snippet snippet = snippetRepository.findByIdAndMemberId(snippetId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        return new SnippetDetailView(
            snippet.getId(),
            snippet.getTitle(),
            snippetMarkdownRenderer.render(snippet.getContentMarkdown()),
            tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), memberId),
            snippet.getUpdatedAt()
        );
    }

    public List<SnippetContentView> allForMember(String username) {
        Long memberId = memberIdFor(username);
        return snippetRepository.searchByMemberIdAndFilters(memberId, "", "").stream()
            .map(snippet -> new SnippetContentView(
                snippet.getId(),
                snippet.getTitle(),
                snippet.getContentMarkdown(),
                tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), memberId),
                snippet.getCreatedAt(),
                snippet.getUpdatedAt()
            ))
            .toList();
    }

    public SnippetContentView contentForMember(String username, Long snippetId) {
        Long memberId = memberIdFor(username);
        Snippet snippet = snippetRepository.findByIdAndMemberId(snippetId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        return new SnippetContentView(
            snippet.getId(),
            snippet.getTitle(),
            snippet.getContentMarkdown(),
            tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), memberId),
            snippet.getCreatedAt(),
            snippet.getUpdatedAt()
        );
    }

    private Long memberIdFor(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return member.getId();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String buildHighlightText(Snippet snippet, String normalizedQuery) {
        String content = snippet.getContentMarkdown() == null ? "" : snippet.getContentMarkdown().replaceAll("\\s+", " ").trim();

        if (content.isEmpty()) {
            return "본문 미리보기가 없습니다.";
        }

        if (!StringUtils.hasText(normalizedQuery)) {
            return abbreviate(content);
        }

        String normalizedContent = content.toLowerCase(Locale.ROOT);
        int index = normalizedContent.indexOf(normalizedQuery);

        if (index < 0) {
            return abbreviate(content);
        }

        int start = Math.max(0, index - 24);
        int end = Math.min(content.length(), index + normalizedQuery.length() + 56);
        String excerpt = content.substring(start, end).trim();

        if (start > 0) {
            excerpt = "..." + excerpt;
        }
        if (end < content.length()) {
            excerpt = excerpt + "...";
        }

        return excerpt;
    }

    private String abbreviate(String content) {
        if (content.length() <= 120) {
            return content;
        }
        return content.substring(0, 117).trim() + "...";
    }
}
