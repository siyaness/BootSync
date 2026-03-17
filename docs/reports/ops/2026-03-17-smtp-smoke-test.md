# SMTP 실메일 스모크 테스트

- 실행 시각: 2026-03-17 15:59 ~ 16:06 KST
- 실행 환경: `local` 프로필, `http://localhost:18080`, Docker MySQL (`localhost:3307`)
- 수행자: 로컬 운영자
- SMTP 제공자: NAVER SMTP
- 발신 주소: `si******@naver.com`
- 대상 메일함: `si******@naver.com`
- 테스트 계정: `smtp_test_0317`

## 설정 확인

- `APP_PUBLIC_BASE_URL=http://localhost:18080`
- `APP_RECOVERY_EMAIL_FROM=si******@naver.com`
- `APP_RECOVERY_EMAIL_MAIL_ENABLED=true`
- `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false`
- `MAIL_HOST=smtp.naver.com`
- `MAIL_PORT=465`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE=true`

## 설계 확인

- 명세는 이메일 링크 진입 시 바로 반영하지 않고, preview 화면에서 사용자가 명시적으로 확인해야 반영되도록 요구한다.
- 구현은 [RecoveryEmailDeliveryService.java](../../../src/main/java/com/bootsync/member/service/RecoveryEmailDeliveryService.java)에서 목적별 제목, 본문, 링크를 분기한다.
- React 확인 화면은 [VerifyEmailPage.tsx](../../../frontend/src/pages/VerifyEmailPage.tsx)에서 `preview -> confirm -> success` 순서로 동작한다.
- API는 [ApiRecoveryEmailVerificationController.java](../../../src/main/java/com/bootsync/api/ApiRecoveryEmailVerificationController.java)에서 `signup`과 `change` 목적을 분리한다.
- 회귀 테스트는 [WebRoutingTest.java](../../../src/test/java/com/bootsync/web/WebRoutingTest.java)에서 signup confirm, change confirm, 교차 목적 invalidation, resend cooldown을 검증한다.

## 수행 절차

1. `docker compose up -d mysql`로 로컬 MySQL을 기동했다.
2. 실SMTP 환경변수를 설정한 뒤 `.\gradlew.bat bootRun --args="--spring.profiles.active=local --server.port=18080"`로 앱을 기동했다.
3. `pwsh -File .\scripts\ops\Invoke-RecoveryEmailSmtpSmokeTest.ps1 -BaseUrl 'http://localhost:18080' -TargetEmail 'siyaness@naver.com' -Username 'smtp_test_0317' -Password 'test12345678' -DisplayName 'SMTP Test' -FromAddress 'siyaness@naver.com' -MailHost 'smtp.naver.com'` 로 회원가입 발송 트리거를 실행했다.
4. 스크립트 출력에서 `pendingVerification: True`를 확인했다.
5. 메일함에서 실제 수신 메일을 확인했다.
6. 메일 본문의 `/app/verify-email?...` 링크를 열어 preview 화면이 먼저 보이고, 확인 버튼을 눌러야 최종 반영되는 것을 확인했다.
7. DB에서 `member.recovery_email`, `member.recovery_email_verified_at`, `recovery_email_verification_token` 소비 상태를 확인했다.

## 결과

- 회원가입 발송 트리거 성공 여부: 성공
- 앱 pending verification 상태 확인: 성공
- 실메일 도착 여부: 성공
- 메일 본문 링크 도메인 일치 여부: 성공 (`http://localhost:18080`)
- preview 후 명시적 confirm 필요 여부: 성공
- confirm 이후 verified 상태 반영 여부: 성공

## 메일 관측 메모

- 메일함에서 확인한 최근 메일 제목은 `[BootSync] 복구 이메일 변경 인증` 이었다.
- 본문에는 `복구 이메일 변경을 완료하려면 아래 링크를 열고 확인 버튼을 눌러 주세요.` 문구와 `purpose=change` 링크가 포함돼 있었다.
- 이는 동일 계정에 대해 후속 `RECOVERY_EMAIL_CHANGE` 토큰이 발급되고 소비된 DB 기록과 일치한다.

## DB 확인 메모

- `member.username = smtp_test_0317`
- `member.recovery_email = siyaness@naver.com`
- `member.recovery_email_verified_at` 값이 비어 있지 않았다.
- `recovery_email_verification_token` 최근 기록:
  - `id=1`, `purpose=SIGNUP_VERIFY`, `issued_at=2026-03-17 15:59:40`, `consumed_at=2026-03-17 16:01:05`
  - `id=6`, `purpose=RECOVERY_EMAIL_CHANGE`, `issued_at=2026-03-17 16:06:08`, `consumed_at=2026-03-17 16:06:20`
- 해석:
  - signup 목적 실메일 발송과 confirm 반영이 먼저 성공했다.
  - 이후 같은 계정의 recovery email change 목적 메일도 실제 발송되고 confirm 반영까지 성공했다.
  - 목적별 제목, 링크, confirm 흐름 분기가 설계대로 동작한 것으로 판단한다.

## 생성된 보조 증적

- 스크립트 초안 보고서: `build/ops-smtp/reports/smtp-smoke-test-20260317-155938.md`
- 로컬 실행 로그: `.\gradlew.bat bootRun --args="--spring.profiles.active=local --server.port=18080"` 콘솔 출력
- DB 확인 명령:
  - `SELECT id, username, recovery_email, recovery_email_verified_at FROM member ORDER BY id DESC LIMIT 5;`
  - `SELECT id, member_id, purpose, target_email, issued_at, expires_at, consumed_at, invalidated_at FROM recovery_email_verification_token ORDER BY id DESC LIMIT 10;`

## 잔여 리스크 / 후속 작업

- 이번 실메일 스모크 테스트는 실제 SMTP 제공자와 실제 메일함을 사용했지만, `APP_PUBLIC_BASE_URL`은 로컬 `http://localhost:18080` 기준이었다.
- AWS 실제 배포 후에는 운영 도메인 기준으로 링크 도메인과 메일 발신 정책을 한 번 더 확인하는 release smoke가 필요하다.
