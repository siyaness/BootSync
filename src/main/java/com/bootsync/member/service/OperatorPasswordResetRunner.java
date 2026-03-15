package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@ConditionalOnProperty(name = "app.operations.password-reset.enabled", havingValue = "true")
public class OperatorPasswordResetRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperatorPasswordResetRunner.class);

    private final AppProperties appProperties;
    private final ConfigurableApplicationContext applicationContext;
    private final OperatorPasswordResetService operatorPasswordResetService;

    public OperatorPasswordResetRunner(
        AppProperties appProperties,
        ConfigurableApplicationContext applicationContext,
        OperatorPasswordResetService operatorPasswordResetService
    ) {
        this.appProperties = appProperties;
        this.applicationContext = applicationContext;
        this.operatorPasswordResetService = operatorPasswordResetService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.PasswordReset passwordReset = appProperties.getOperations().getPasswordReset();
        String username = requireText(passwordReset.getUsername(), "app.operations.password-reset.username");
        String actor = requireText(passwordReset.getActor(), "app.operations.password-reset.actor");
        String reason = requireText(passwordReset.getReason(), "app.operations.password-reset.reason");
        String temporaryPassword = resolveTemporaryPassword(passwordReset);

        OperatorPasswordResetService.OperatorPasswordResetResult result = operatorPasswordResetService.resetPassword(
            username,
            temporaryPassword
        );

        log.info(
            "BootSync operator password reset completed: memberId={}, username={}, recoveryEmail={}, actor={}, reason={}",
            result.memberId(),
            result.username(),
            result.maskedRecoveryEmail(),
            actor,
            reason
        );

        if (passwordReset.isCloseContextAfterRun()) {
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }

    private String resolveTemporaryPassword(AppProperties.PasswordReset passwordReset) {
        String inlinePassword = passwordReset.getTemporaryPassword() == null ? "" : passwordReset.getTemporaryPassword().trim();
        String passwordFile = passwordReset.getTemporaryPasswordFile() == null ? "" : passwordReset.getTemporaryPasswordFile().trim();

        if (!inlinePassword.isBlank() && !passwordFile.isBlank()) {
            throw new IllegalStateException(
                "임시 비밀번호는 inline 값과 파일 경로 중 하나만 지정할 수 있습니다."
            );
        }

        if (!passwordFile.isBlank()) {
            try {
                return requireText(Files.readString(Path.of(passwordFile), StandardCharsets.UTF_8), passwordFile);
            } catch (IOException exception) {
                throw new IllegalStateException("임시 비밀번호 파일을 읽을 수 없습니다: " + passwordFile, exception);
            }
        }

        return requireText(inlinePassword, "app.operations.password-reset.temporary-password");
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalStateException("필수 운영 설정이 비어 있습니다: " + fieldName);
        }
        return value.trim();
    }
}
