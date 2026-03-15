package com.bootsync.member.service;

import com.bootsync.common.time.AppProperties;
import com.bootsync.member.dto.RecoveryEmailVerificationPreviewLink;
import com.bootsync.member.entity.Member;
import com.bootsync.member.entity.RecoveryEmailVerificationPurpose;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class RecoveryEmailDeliveryService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final RecoveryEmailVerificationPreviewStore previewStore;
    private final AppProperties appProperties;

    public RecoveryEmailDeliveryService(
        ObjectProvider<JavaMailSender> javaMailSenderProvider,
        RecoveryEmailVerificationPreviewStore previewStore,
        AppProperties appProperties
    ) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.previewStore = previewStore;
        this.appProperties = appProperties;
    }

    public void deliver(Member member, RecoveryEmailVerificationPurpose purpose, String targetEmail, String rawToken) {
        String relativePath = relativePathFor(purpose, rawToken);
        if (appProperties.getRecoveryEmail().isDevelopmentPreviewEnabled()) {
            previewStore.remember(member.getId(), purpose, new RecoveryEmailVerificationPreviewLink(rawToken, relativePath));
        }

        if (!appProperties.getRecoveryEmail().isMailEnabled()) {
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new IllegalStateException("복구 이메일 발송기가 설정되지 않았습니다.");
        }

        String absoluteUrl = absoluteUrl(relativePath);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.getRecoveryEmail().getFromAddress());
        message.setTo(targetEmail);
        message.setSubject(subjectFor(purpose));
        message.setText(bodyFor(purpose, absoluteUrl));

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("복구 이메일 인증 메일 발송에 실패했습니다.", exception);
        }
    }

    private String relativePathFor(RecoveryEmailVerificationPurpose purpose, String rawToken) {
        return switch (purpose) {
            case SIGNUP_VERIFY -> "/app/verify-email?purpose=signup&token=" + rawToken;
            case RECOVERY_EMAIL_CHANGE -> "/app/verify-email?purpose=change&token=" + rawToken;
        };
    }

    private String subjectFor(RecoveryEmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP_VERIFY -> "[BootSync] 회원가입 복구 이메일 인증";
            case RECOVERY_EMAIL_CHANGE -> "[BootSync] 복구 이메일 변경 인증";
        };
    }

    private String bodyFor(RecoveryEmailVerificationPurpose purpose, String absoluteUrl) {
        String intro = switch (purpose) {
            case SIGNUP_VERIFY -> "회원가입을 마무리하려면 아래 링크를 열고 확인 버튼을 눌러 주세요.";
            case RECOVERY_EMAIL_CHANGE -> "복구 이메일 변경을 완료하려면 아래 링크를 열고 확인 버튼을 눌러 주세요.";
        };

        return intro + System.lineSeparator()
            + System.lineSeparator()
            + absoluteUrl + System.lineSeparator()
            + System.lineSeparator()
            + "링크를 열어도 바로 적용되지 않고, 확인 화면에서 명시적으로 확정해야 반영됩니다.";
    }

    private String absoluteUrl(String relativePath) {
        String baseUrl = appProperties.getRecoveryEmail().getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + relativePath;
        }
        return baseUrl + relativePath;
    }
}
