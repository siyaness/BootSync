# BootSync Prod Environment Checklist

## 1. 문서 목적

이 문서는 BootSync 운영 배포에 필요한 환경변수를 코드 기준으로 정리한 체크리스트다.

- 기준 파일: [application.yml](../../src/main/resources/application.yml), [application-prod.yml](../../src/main/resources/application-prod.yml)
- 목적: `배포 직전 어떤 값을 넣어야 하는지`를 한 번에 확인하기 위함
- 이 문서는 값의 존재 여부와 용도를 정리하는 문서이고, 실제 값은 저장소에 커밋하지 않는다.

## 2. 기본 원칙

- 운영에서는 `SPRING_PROFILES_ACTIVE=prod`를 명시하는 것을 권장한다.
- DB 비밀번호, SMTP 비밀번호, HMAC 시크릿은 문서에 실값을 적지 않는다.
- 실제 운영 값은 `.env`, Kubernetes Secret, AWS SSM Parameter Store, Secrets Manager 중 하나로 주입한다.

## 3. 필수 앱 런타임 변수

아래 값이 없으면 운영 배포가 사실상 불가능하다.

| 변수 | 필수 | 예시 | 용도 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 권장 | `prod` | 운영 프로필 명시 |
| `DB_URL` | 예 | `jdbc:mysql://.../bootsync?...` | 운영 DB 연결 |
| `DB_USERNAME` | 예 | `bootsync_app` | 운영 DB 계정 |
| `DB_PASSWORD` | 예 | 비공개 | 운영 DB 비밀번호 |
| `APP_PUBLIC_BASE_URL` | 예 | `https://bootsync.example.com` | recovery email 링크 생성 기준 |
| `APP_RECOVERY_EMAIL_FROM` | 예 | `no-reply@bootsync.example.com` | 발신 주소 |
| `APP_RECOVERY_EMAIL_MAIL_ENABLED` | 예 | `true` | 운영 실메일 활성화 |
| `MAIL_HOST` | 예 | `smtp.example.com` | SMTP 서버 |
| `MAIL_PORT` | 예 | `587` | SMTP 포트 |
| `MAIL_USERNAME` | 예 | `smtp-user` | SMTP 사용자 |
| `MAIL_PASSWORD` | 예 | 비공개 | SMTP 비밀번호 |
| `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN` | 모니터링 사용 시 예 | 비공개 | `/actuator/prometheus` Bearer 토큰 |

## 4. 강력 권장 변수

아래 값은 필수는 아니지만 운영 기준으로는 거의 넣는 편이 좋다.

| 변수 | 권장값 | 용도 |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Seoul` | 앱 기준 시간대 |
| `SESSION_COOKIE_NAME` | `BOOTSYNCSESSION` | 세션 쿠키 이름 |
| `APP_AUDIT_REQUEST_IP_HMAC_SECRET` | 비공개 | 출결 감사 로그 IP HMAC |
| `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN` | 비공개 | 내부 Prometheus scrape 인증 |
| `APP_AUDIT_REQUEST_IP_HMAC_RETENTION_DAYS` | `30` | HMAC 보존일 |
| `APP_AUDIT_REQUEST_IP_HMAC_PRUNE_ENABLED` | `true` | 오래된 HMAC 정리 |
| `APP_AUDIT_REQUEST_IP_HMAC_PRUNE_CRON` | `0 25 3 * * *` | HMAC prune 스케줄 |
| `APP_SECURITY_TRUST_FORWARDED_HEADERS` | `true` | 프록시/Ingress 뒤에서 실제 IP 해석 |

주의:

- `APP_SECURITY_TRUST_FORWARDED_HEADERS=true`는 프록시가 `X-Forwarded-For`를 정규화해주는 환경에서만 켠다.

## 5. 기본값으로 두어도 되는 항목

아래 값은 기본값이 있으므로 필요할 때만 명시해도 된다.

| 변수 | 기본값 | 비고 |
|---|---|---|
| `APP_TIMEZONE` | `Asia/Seoul` | 운영에서는 그대로 사용 권장 |
| `SESSION_COOKIE_NAME` | `BOOTSYNCSESSION` | 특별한 이유 없으면 유지 |
| `MAIL_PORT` | `587` | `application-prod.yml` 기준 |

## 6. 운영에서 꺼두어야 하는 값

아래 값은 운영에서는 기본적으로 꺼둔다.

| 변수 | 운영 권장값 | 이유 |
|---|---|---|
| `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED` | `false` | 로컬 확인 링크 비노출 |
| `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED` | `false` | 일회성 maintenance path |
| `APP_OPERATIONS_PASSWORD_RESET_ENABLED` | `false` | 일회성 maintenance path |
| `APP_OPERATIONS_DELETION_CANCEL_ENABLED` | `false` | 일회성 maintenance path |
| `BOOTSYNC_DEMO_USERNAME` | unset | 로컬 시드 전용 계정 |
| `BOOTSYNC_DEMO_PASSWORD` | unset | 로컬 시드 전용 계정 |

## 7. 상황별로 잠깐만 쓰는 maintenance 변수

아래 값은 상시 운영값이 아니라, 필요할 때만 잠깐 주입한다.

### 7.1 purge one-shot

| 변수 | 용도 |
|---|---|
| `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED` | one-shot purge 활성화 |
| `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ACTOR` | 수행자 |
| `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_REASON` | 수행 사유 |
| `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_CLOSE_CONTEXT_AFTER_RUN` | 기본 `true` |

### 7.2 운영자 보조 비밀번호 초기화

| 변수 | 용도 |
|---|---|
| `APP_OPERATIONS_PASSWORD_RESET_ENABLED` | runner 활성화 |
| `APP_OPERATIONS_PASSWORD_RESET_USERNAME` | 대상 username |
| `APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD` | 임시 비밀번호 직접 지정 |
| `APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD_FILE` | 임시 비밀번호 파일 경로 |
| `APP_OPERATIONS_PASSWORD_RESET_ACTOR` | 수행자 |
| `APP_OPERATIONS_PASSWORD_RESET_REASON` | 수행 사유 |
| `APP_OPERATIONS_PASSWORD_RESET_CLOSE_CONTEXT_AFTER_RUN` | 기본 `true` |

### 7.3 운영자 보조 삭제 취소

| 변수 | 용도 |
|---|---|
| `APP_OPERATIONS_DELETION_CANCEL_ENABLED` | runner 활성화 |
| `APP_OPERATIONS_DELETION_CANCEL_USERNAME` | 대상 username |
| `APP_OPERATIONS_DELETION_CANCEL_ACTOR` | 수행자 |
| `APP_OPERATIONS_DELETION_CANCEL_REASON` | 수행 사유 |
| `APP_OPERATIONS_DELETION_CANCEL_CLOSE_CONTEXT_AFTER_RUN` | 기본 `true` |

## 8. 앱 바깥에서 쓰는 운영 변수

아래 값은 Spring 앱 자체보다 백업 스크립트/운영 자동화에서 주로 사용한다.

| 변수 | 필수 여부 | 용도 |
|---|---|---|
| `BACKUP_S3_BUCKET` | S3 업로드 시 필수 | 백업 버킷 이름 |
| `AWS_REGION` | S3 업로드 시 필수 | AWS 리전 |
| `AWS_PROFILE` | 선택 | 로컬 또는 운영 CLI profile |

주의:

- 현재 [Invoke-MySqlBackupToS3.ps1](../../scripts/ops/Invoke-MySqlBackupToS3.ps1)와 [Invoke-MySqlRestoreFromS3.ps1](../../scripts/ops/Invoke-MySqlRestoreFromS3.ps1)는 `docker exec/cp`로 MySQL 컨테이너를 기준으로 동작한다.
- 최종 운영 DB가 `RDS`라면, 이 값들만 넣는다고 바로 운영 백업이 완성되는 것은 아니다.

## 9. prod 예시 템플릿

실값 없이 형태만 정리하면 아래처럼 본다.

```text
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/bootsync?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>

APP_TIMEZONE=Asia/Seoul
SESSION_COOKIE_NAME=BOOTSYNCSESSION

APP_PUBLIC_BASE_URL=https://<YOUR_DOMAIN>
APP_RECOVERY_EMAIL_FROM=no-reply@<YOUR_DOMAIN>
APP_RECOVERY_EMAIL_MAIL_ENABLED=true
APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false

MAIL_HOST=<SMTP_HOST>
MAIL_PORT=587
MAIL_USERNAME=<SMTP_USERNAME>
MAIL_PASSWORD=<SMTP_PASSWORD>

APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN=<RANDOM_PROMETHEUS_TOKEN>

APP_AUDIT_REQUEST_IP_HMAC_SECRET=<RANDOM_SECRET>
APP_AUDIT_REQUEST_IP_HMAC_RETENTION_DAYS=30
APP_AUDIT_REQUEST_IP_HMAC_PRUNE_ENABLED=true
APP_AUDIT_REQUEST_IP_HMAC_PRUNE_CRON=0 25 3 * * *

APP_SECURITY_TRUST_FORWARDED_HEADERS=true

APP_ACCOUNT_DELETION_PURGE_ENABLED=true
APP_ACCOUNT_DELETION_PURGE_CRON=0 15 3 * * *
```

## 10. 운영 배포 직전 체크

배포 직전 아래만 빠르게 다시 확인한다.

1. `SPRING_PROFILES_ACTIVE=prod`
2. `DB_*` 3종 입력 완료
3. `APP_PUBLIC_BASE_URL`이 실제 접속 주소와 일치
4. `APP_RECOVERY_EMAIL_MAIL_ENABLED=true`
5. `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false`
6. `MAIL_*` 4종 입력 완료
7. Prometheus를 쓸 경우 `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN` 준비 완료
8. `APP_AUDIT_REQUEST_IP_HMAC_SECRET` 준비 완료
9. `APP_SECURITY_TRUST_FORWARDED_HEADERS=true` 여부를 프록시 구조에 맞게 결정
10. maintenance runner 관련 `*_ENABLED`가 실수로 켜져 있지 않은지 확인

## 11. 같이 볼 문서

- [AWS_DEPLOYMENT_CHECKLIST.md](../planning/AWS_DEPLOYMENT_CHECKLIST.md)
- [AWS_FINAL_PROJECT_GUIDE.md](../planning/AWS_FINAL_PROJECT_GUIDE.md)
- [OPERATIONS_RUNBOOK.md](OPERATIONS_RUNBOOK.md)
- [README.md](../../README.md)
