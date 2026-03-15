package com.bootsync.snippet.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.bootsync.member.entity.Member;
import com.bootsync.member.repository.MemberRepository;
import com.bootsync.snippet.dto.SnippetFormRequest;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.entity.Tag;
import com.bootsync.tag.repository.TagRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SnippetCommandService {

    private final MemberRepository memberRepository;
    private final SnippetRepository snippetRepository;
    private final SnippetTagRepository snippetTagRepository;
    private final TagRepository tagRepository;
    private final SnippetSecretWarningService snippetSecretWarningService;
    private final Clock clock;

    public SnippetCommandService(
        MemberRepository memberRepository,
        SnippetRepository snippetRepository,
        SnippetTagRepository snippetTagRepository,
        TagRepository tagRepository,
        SnippetSecretWarningService snippetSecretWarningService,
        Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.snippetRepository = snippetRepository;
        this.snippetTagRepository = snippetTagRepository;
        this.tagRepository = tagRepository;
        this.snippetSecretWarningService = snippetSecretWarningService;
        this.clock = clock;
    }

    public SnippetFormRequest blankForm() {
        return new SnippetFormRequest("", "", "", "");
    }

    public SnippetFormRequest formForMember(String username, Long snippetId) {
        Long memberId = memberIdFor(username);
        Snippet snippet = snippetRepository.findByIdAndMemberId(snippetId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        return new SnippetFormRequest(
            snippet.getTitle(),
            snippet.getContentMarkdown(),
            String.join(", ", tagRepository.findNamesBySnippetIdAndMemberId(snippet.getId(), memberId)),
            ""
        );
    }

    @Transactional
    public Long createForMember(String username, String sessionId, SnippetFormRequest snippetFormRequest) {
        Long memberId = memberIdFor(username);
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> normalizedTags = parseTagNames(snippetFormRequest.tagNames());
        snippetSecretWarningService.validateOrThrow(username, sessionId, snippetFormRequest);

        Snippet snippet = new Snippet();
        snippet.setMemberId(memberId);
        snippet.setTitle(snippetFormRequest.title().trim());
        snippet.setContentMarkdown(snippetFormRequest.contentMarkdown());
        snippet.setCreatedAt(now);
        snippet.setUpdatedAt(now);

        Snippet savedSnippet = snippetRepository.save(snippet);
        syncTags(memberId, savedSnippet.getId(), normalizedTags);
        return savedSnippet.getId();
    }

    @Transactional
    public void updateForMember(String username, String sessionId, Long snippetId, SnippetFormRequest snippetFormRequest) {
        Long memberId = memberIdFor(username);
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> normalizedTags = parseTagNames(snippetFormRequest.tagNames());
        snippetSecretWarningService.validateOrThrow(username, sessionId, snippetFormRequest);

        Snippet snippet = snippetRepository.findByIdAndMemberId(snippetId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        snippet.setTitle(snippetFormRequest.title().trim());
        snippet.setContentMarkdown(snippetFormRequest.contentMarkdown());
        snippet.setUpdatedAt(now);

        snippetRepository.save(snippet);
        syncTags(memberId, snippet.getId(), normalizedTags);
    }

    @Transactional
    public void deleteForMember(String username, Long snippetId) {
        Long memberId = memberIdFor(username);
        Snippet snippet = snippetRepository.findByIdAndMemberId(snippetId, memberId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        // Delete join rows explicitly so JPA create-drop test schema and MySQL behave the same way.
        snippetTagRepository.deleteByIdMemberIdAndIdSnippetId(memberId, snippetId);
        snippetRepository.delete(snippet);
        cleanupOrphanTags(memberId);
    }

    private void syncTags(Long memberId, Long snippetId, List<String> normalizedTags) {
        snippetTagRepository.deleteByIdMemberIdAndIdSnippetId(memberId, snippetId);

        if (!normalizedTags.isEmpty()) {
            List<SnippetTag> links = normalizedTags.stream()
                .map(tagName -> link(memberId, snippetId, findOrCreateTag(memberId, tagName).getId()))
                .toList();

            snippetTagRepository.saveAll(links);
        }

        cleanupOrphanTags(memberId);
    }

    private Tag findOrCreateTag(Long memberId, String normalizedTag) {
        return tagRepository.findByMemberIdAndNormalizedName(memberId, normalizedTag)
            .orElseGet(() -> {
                Tag tag = new Tag();
                tag.setMemberId(memberId);
                tag.setName(normalizedTag);
                tag.setNormalizedName(normalizedTag);
                return tagRepository.save(tag);
            });
    }

    private SnippetTag link(Long memberId, Long snippetId, Long tagId) {
        SnippetTag snippetTag = new SnippetTag();
        snippetTag.setId(new SnippetTagId(memberId, snippetId, tagId));
        return snippetTag;
    }

    private void cleanupOrphanTags(Long memberId) {
        List<Long> orphanedTagIds = tagRepository.findOrphanedIdsByMemberId(memberId);
        if (!orphanedTagIds.isEmpty()) {
            tagRepository.deleteAllByIdInBatch(orphanedTagIds);
        }
    }

    private List<String> parseTagNames(String rawTagNames) {
        if (!StringUtils.hasText(rawTagNames)) {
            return List.of();
        }

        Set<String> normalizedTags = new LinkedHashSet<>();

        for (String token : rawTagNames.split("[,\\n]")) {
            String trimmed = token.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }

            String normalized = trimmed.toLowerCase(Locale.ROOT);
            if (normalized.length() > 20) {
                throw new SnippetFormValidationException("tagNames", "태그는 각 항목당 20자 이하여야 합니다.");
            }
            if (normalizedTags.size() >= 10 && !normalizedTags.contains(normalized)) {
                throw new SnippetFormValidationException("tagNames", "태그는 최대 10개까지 저장할 수 있습니다.");
            }

            normalizedTags.add(normalized);
        }

        return List.copyOf(normalizedTags);
    }

    private Long memberIdFor(String username) {
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return member.getId();
    }
}
