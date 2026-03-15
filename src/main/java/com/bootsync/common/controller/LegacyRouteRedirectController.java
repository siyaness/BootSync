package com.bootsync.common.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.YearMonth;

@Controller
public class LegacyRouteRedirectController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return isAuthenticated(authentication) ? "redirect:/app/dashboard" : "redirect:/app/login";
    }

    @GetMapping("/signup")
    public String signup(Authentication authentication) {
        return isAuthenticated(authentication) ? "redirect:/app/dashboard" : "redirect:/app/signup";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/app/dashboard";
    }

    @GetMapping("/attendance")
    public String attendance(
        @RequestParam(name = "yearMonth", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
        @RequestParam(name = "editId", required = false) Long editId
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/app/attendance");
        if (yearMonth != null) {
            builder.queryParam("yearMonth", yearMonth);
        }
        if (editId != null) {
            builder.queryParam("editId", editId);
        }
        return "redirect:" + builder.build().toUriString();
    }

    @GetMapping("/snippets")
    public String snippets(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "tag", required = false) String tag
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/app/snippets");
        if (query != null && !query.isBlank()) {
            builder.queryParam("q", query);
        }
        if (tag != null && !tag.isBlank()) {
            builder.queryParam("tag", tag);
        }
        return "redirect:" + builder.build().toUriString();
    }

    @GetMapping("/snippets/new")
    public String newSnippet() {
        return "redirect:/app/snippets/new";
    }

    @GetMapping("/snippets/{id}")
    public String snippetDetail(@PathVariable Long id) {
        return "redirect:/app/snippets/" + id;
    }

    @GetMapping("/snippets/{id}/edit")
    public String editSnippet(@PathVariable Long id) {
        return "redirect:/app/snippets/" + id + "/edit";
    }

    @GetMapping("/settings")
    public String settings() {
        return "redirect:/app/settings";
    }

    @GetMapping("/auth/recovery-email/verify")
    public String signupRecoveryEmailVerify(@RequestParam("token") String token) {
        return "redirect:" + UriComponentsBuilder.fromPath("/app/verify-email")
            .queryParam("purpose", "signup")
            .queryParam("token", token)
            .build()
            .toUriString();
    }

    @GetMapping("/settings/recovery-email/verify")
    public String settingsRecoveryEmailVerify(@RequestParam("token") String token) {
        return "redirect:" + UriComponentsBuilder.fromPath("/app/verify-email")
            .queryParam("purpose", "change")
            .queryParam("token", token)
            .build()
            .toUriString();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
