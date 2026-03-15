package com.bootsync.snippet.service;

import com.bootsync.snippet.dto.SnippetFormRequest;
import com.bootsync.snippet.dto.SnippetSecretWarningResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SnippetSecretWarningService {

    private static final int WARNING_TTL_MINUTES = 10;
    private static final List<SecretPattern> SECRET_PATTERNS = List.of(
        new SecretPattern("AWS_ACCESS_KEY", Pattern.compile("AKIA[0-9A-Z]{16}")),
        new SecretPattern("PRIVATE_KEY", Pattern.compile("-----BEGIN(?: [A-Z]+)? PRIVATE KEY-----")),
        new SecretPattern("JWT", Pattern.compile("eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")),
        new SecretPattern("GITHUB_TOKEN", Pattern.compile("gh[pousr]_[A-Za-z0-9]{20,}"))
    );

    private final Clock clock;
    private final Map<String, PendingSecretWarning> warnings = new ConcurrentHashMap<>();

    public SnippetSecretWarningService(Clock clock) {
        this.clock = clock;
    }

    public void validateOrThrow(String username, String sessionId, SnippetFormRequest snippetFormRequest) {
        pruneExpiredWarnings();

        List<String> warningCodes = detectWarningCodes(snippetFormRequest.contentMarkdown());
        if (warningCodes.isEmpty()) {
            return;
        }

        String contentFingerprint = fingerprint(snippetFormRequest.contentMarkdown());
        if (hasValidAcknowledgement(username, sessionId, contentFingerprint, snippetFormRequest.secretWarningToken())) {
            return;
        }

        throw new SnippetSecretWarningRequiredException(issueWarning(username, sessionId, contentFingerprint, warningCodes));
    }

    private List<String> detectWarningCodes(String contentMarkdown) {
        return SECRET_PATTERNS.stream()
            .filter(secretPattern -> secretPattern.pattern().matcher(contentMarkdown).find())
            .map(SecretPattern::code)
            .toList();
    }

    private boolean hasValidAcknowledgement(String username, String sessionId, String contentFingerprint, String secretWarningToken) {
        if (secretWarningToken == null || secretWarningToken.isBlank()) {
            return false;
        }

        PendingSecretWarning warning = warnings.get(secretWarningToken);
        LocalDateTime now = LocalDateTime.now(clock);
        if (warning == null || warning.consumed() || warning.expiresAt().isBefore(now)) {
            return false;
        }

        if (!warning.username().equals(username) || !warning.sessionId().equals(sessionId)) {
            return false;
        }

        if (!warning.contentFingerprint().equals(contentFingerprint)) {
            return false;
        }

        warnings.put(secretWarningToken, warning.consume(now));
        return true;
    }

    private SnippetSecretWarningResponse issueWarning(
        String username,
        String sessionId,
        String contentFingerprint,
        List<String> warningCodes
    ) {
        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(WARNING_TTL_MINUTES);
        warnings.put(token, new PendingSecretWarning(username, sessionId, contentFingerprint, expiresAt, false, null));

        return new SnippetSecretWarningResponse(
            warningCodes,
            "민감 정보로 보이는 내용이 감지되었습니다. 경고를 확인한 뒤 같은 내용으로 다시 제출해야 저장할 수 있습니다.",
            true,
            token,
            contentFingerprint,
            expiresAt
        );
    }

    private void pruneExpiredWarnings() {
        LocalDateTime now = LocalDateTime.now(clock);
        warnings.entrySet().removeIf(entry -> {
            PendingSecretWarning warning = entry.getValue();
            return warning.expiresAt().isBefore(now) || warning.consumed();
        });
    }

    private String fingerprint(String contentMarkdown) {
        String normalized = contentMarkdown.replace("\r\n", "\n").trim();
        return sha256(normalized);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    private record SecretPattern(String code, Pattern pattern) {
    }

    private record PendingSecretWarning(
        String username,
        String sessionId,
        String contentFingerprint,
        LocalDateTime expiresAt,
        boolean consumed,
        LocalDateTime consumedAt
    ) {
        private PendingSecretWarning consume(LocalDateTime consumedAt) {
            return new PendingSecretWarning(username, sessionId, contentFingerprint, expiresAt, true, consumedAt);
        }
    }
}
