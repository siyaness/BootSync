package com.bootsync.member.service;

import com.bootsync.member.repository.MemberRepository;
import com.bootsync.snippet.entity.Snippet;
import com.bootsync.snippet.entity.SnippetTag;
import com.bootsync.snippet.entity.SnippetTagId;
import com.bootsync.snippet.repository.SnippetRepository;
import com.bootsync.snippet.repository.SnippetTagRepository;
import com.bootsync.tag.entity.Tag;
import com.bootsync.tag.repository.TagRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Configuration
@Profile({"local", "test"})
public class DemoSnippetInitializer {

    @Bean
    @Order(20)
    ApplicationRunner demoSnippetApplicationRunner(
        MemberRepository memberRepository,
        SnippetRepository snippetRepository,
        TagRepository tagRepository,
        SnippetTagRepository snippetTagRepository,
        Clock clock,
        @Value("${spring.security.user.name:d}") String demoUsername
    ) {
        return args -> {
            Long memberId = memberRepository.findByUsername(demoUsername)
                .map(member -> member.getId())
                .orElse(null);

            if (memberId == null || snippetRepository.countByMemberId(memberId) > 0) {
                return;
            }

            Tag springTag = getOrCreateTag(tagRepository, memberId, "spring");
            Tag mysqlTag = getOrCreateTag(tagRepository, memberId, "mysql");
            Tag securityTag = getOrCreateTag(tagRepository, memberId, "security");

            LocalDateTime now = LocalDateTime.now(clock);

            Snippet loginSnippet = new Snippet();
            loginSnippet.setMemberId(memberId);
            loginSnippet.setTitle("Spring Security 로그인 메모");
            loginSnippet.setContentMarkdown("""
                - 기본 로그인 페이지 대신 `/login`을 사용한다.
                - 로그인 성공 후 `/dashboard`로 이동한다.
                - 테스트 프로필에서는 `d / d`로 빠르게 확인할 수 있다.
                """);
            loginSnippet.setCreatedAt(now.minusDays(3));
            loginSnippet.setUpdatedAt(now.minusDays(1));

            Snippet mysqlSnippet = new Snippet();
            mysqlSnippet.setMemberId(memberId);
            mysqlSnippet.setTitle("MySQL 로컬 실행 체크");
            mysqlSnippet.setContentMarkdown("""
                docker compose up -d mysql
                DB_URL=jdbc:mysql://localhost:3307/bootsync
                앱은 local 프로필로 실행한다.
                """);
            mysqlSnippet.setCreatedAt(now.minusDays(2));
            mysqlSnippet.setUpdatedAt(now.minusHours(6));

            Snippet bootRunSnippet = new Snippet();
            bootRunSnippet.setMemberId(memberId);
            bootRunSnippet.setTitle("bootRunTestProfile 팁");
            bootRunSnippet.setContentMarkdown("""
                `./gradlew.bat bootRunTestProfile`
                포트 충돌 시 `--args="--server.port=18080"`를 붙인다.
                """);
            bootRunSnippet.setCreatedAt(now.minusDays(1));
            bootRunSnippet.setUpdatedAt(now.minusHours(2));

            List<Snippet> savedSnippets = snippetRepository.saveAll(List.of(loginSnippet, mysqlSnippet, bootRunSnippet));
            Map<String, Snippet> snippetsByTitle = savedSnippets.stream()
                .collect(java.util.stream.Collectors.toMap(Snippet::getTitle, Function.identity()));

            snippetTagRepository.saveAll(List.of(
                link(memberId, snippetsByTitle.get("Spring Security 로그인 메모").getId(), springTag.getId()),
                link(memberId, snippetsByTitle.get("Spring Security 로그인 메모").getId(), securityTag.getId()),
                link(memberId, snippetsByTitle.get("MySQL 로컬 실행 체크").getId(), mysqlTag.getId()),
                link(memberId, snippetsByTitle.get("bootRunTestProfile 팁").getId(), springTag.getId())
            ));
        };
    }

    private Tag getOrCreateTag(TagRepository tagRepository, Long memberId, String name) {
        return tagRepository.findByMemberIdAndNormalizedName(memberId, name)
            .orElseGet(() -> {
                Tag tag = new Tag();
                tag.setMemberId(memberId);
                tag.setName(name);
                tag.setNormalizedName(name);
                return tagRepository.save(tag);
            });
    }

    private SnippetTag link(Long memberId, Long snippetId, Long tagId) {
        SnippetTag snippetTag = new SnippetTag();
        snippetTag.setId(new SnippetTagId(memberId, snippetId, tagId));
        return snippetTag;
    }
}
