package com.bootsync.member.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MemberPasswordPolicy {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password",
        "password123",
        "password1234",
        "qwerty123",
        "qwerty1234",
        "1234567890",
        "letmein123",
        "welcome123",
        "admin1234"
    );

    public void validate(String rawPassword, String fieldName) {
        String normalized = rawPassword.trim().toLowerCase(Locale.ROOT);

        if (COMMON_PASSWORDS.contains(normalized)) {
            throw new MemberValidationException(fieldName, "너무 흔한 비밀번호는 사용할 수 없습니다.");
        }

        if (hasSingleRepeatedCharacter(normalized)) {
            throw new MemberValidationException(fieldName, "너무 단순한 반복 패턴의 비밀번호는 사용할 수 없습니다.");
        }
    }

    private boolean hasSingleRepeatedCharacter(String rawPassword) {
        return rawPassword.chars().distinct().count() == 1;
    }
}
