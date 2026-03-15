package com.bootsync.snippet.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SnippetMarkdownRenderer {

    private static final PolicyFactory MARKDOWN_POLICY = new HtmlPolicyBuilder()
        .allowStandardUrlProtocols()
        .allowElements(
            "p", "br", "ul", "ol", "li", "blockquote", "pre", "code",
            "strong", "em", "a", "h1", "h2", "h3", "h4", "h5", "h6", "hr"
        )
        .allowAttributes("href").onElements("a")
        .requireRelNofollowOnLinks()
        .toFactory();

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .sanitizeUrls(true)
        .softbreak("<br />")
        .build();

    public String render(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }

        Node document = parser.parse(markdown);
        return MARKDOWN_POLICY.sanitize(htmlRenderer.render(document));
    }
}
