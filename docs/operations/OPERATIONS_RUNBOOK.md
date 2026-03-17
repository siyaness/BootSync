# BootSync Operations Runbook

기준 시점: 2026-03-15

이 문서는 현재 코드베이스 기준으로 운영자가 따라야 하는 보안/계정/복구 절차를 정리한 runbook이다.
지금 시점의 핵심 범위는 아래 다섯 가지다.

- `attendance_audit_log.request_ip_hmac` 보존 및 키 회전
- recovery email SMTP 실메일 점검
- 삭제 요청 등록부 운영과 삭제 취소 절차
- recovery email 기반 운영자 보조 계정 복구 절차
- 백업/복원 및 삭제 계정 scrub 절차

중요: 이 문서는 현재 구현과 명세를 맞추기 위한 운영 절차 문서다. 실제 공개 출시 기준은 아래 항목이 별도로 충족돼야 한다.

- purge 스케줄 운영 첫 실행 기록 확보
- AWS 실제 배포(`ECR -> RDS -> EC2/k3s`) 진행
- 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치
- prod-like 복원 리허설 수행과 `RTO 8시간` 실측
- 개인정보 처리방침 / 이용약관 / 삭제·복구 정책 확정과 공개 운영 값 반영

## 1. request_ip_hmac 운영 규칙

### 목적

- 출결 저장/수정/삭제 요청의 IP 원문을 남기지 않고, 필요할 때만 서버 비밀키 기반 `HMAC-SHA256` 결과를 감사 로그에 남긴다.
- `request_ip_hmac`는 직접 식별 가능성이 있는 값으로 간주하고 기본 보존기간 `30일`을 넘기지 않는다.

### 관련 설정

```powershell
$env:APP_AUDIT_REQUEST_IP_HMAC_SECRET='change-this-secret'
$env:APP_AUDIT_REQUEST_IP_HMAC_RETENTION_DAYS='30'
$env:APP_AUDIT_REQUEST_IP_HMAC_PRUNE_ENABLED='true'
$env:APP_AUDIT_REQUEST_IP_HMAC_PRUNE_CRON='0 25 3 * * *'
```

- `APP_AUDIT_REQUEST_IP_HMAC_SECRET`
  값이 없으면 `request_ip_hmac`는 저장하지 않는다.
- `APP_AUDIT_REQUEST_IP_HMAC_RETENTION_DAYS`
  기본값은 `30`이다.
- `APP_AUDIT_REQUEST_IP_HMAC_PRUNE_ENABLED`
  기본값은 `true`다.
- `APP_AUDIT_REQUEST_IP_HMAC_PRUNE_CRON`
  기본값은 KST 기준 매일 `03:25`다.

### 현재 구현 동작

- 출결 `CREATE`, `UPDATE`, `DELETE` 시 현재 요청 IP를 `HMAC-SHA256`으로 변환해 `request_ip_hmac`에 저장한다.
- 원문 IP는 DB에 저장하지 않는다.
- 스케줄 잡이 `changed_at < now - retentionDays` 인 레코드의 `request_ip_hmac`를 `NULL`로 비운다.
- 계정 purge 시에도 `request_ip_hmac`는 함께 비식별화된다.

### 키 보관 원칙

- 비밀키는 저장소에 커밋하지 않는다.
- 운영에서는 AWS Secrets Manager 또는 SSM Parameter Store를 우선 사용한다.
- 평문 비밀키를 운영 위키, 채팅, 이슈 댓글에 남기지 않는다.
- 비밀키 접근 권한은 실제 운영 담당자에게만 최소 범위로 부여한다.

### 키 회전 절차

중요: 현재 스키마에는 `request_ip_hmac`의 키 버전 컬럼이 없다. 따라서 회전 시각을 별도 운영 기록에 남겨야 한다.

1. 새 비밀키를 생성한다.
2. 운영 기록에 아래 값을 남긴다.
   - `rotation_started_at`
   - `new_key_label`
   - `old_key_label`
   - 수행자
   - 사유
3. 새 비밀키를 시크릿 저장소에 반영한다.
4. 앱을 재배포하거나 환경변수를 재적용한다.
5. 배포 직후 출결 저장 스모크 테스트를 수행한다.
6. `request_ip_hmac`가 새로 생성되는지 확인한다.
7. 이전 비밀키는 최대 `30일`까지만 보관한다.
8. 마지막 구키 생성 레코드가 보존기간을 모두 지나면 이전 비밀키를 삭제한다.

### 조사 시 주의사항

- 회전 전후 로그는 같은 비밀키로 생성되지 않았을 수 있으므로 직접 문자열 비교가 항상 의미 있지 않다.
- 비교가 필요한 경우 `changed_at`과 운영 기록의 `rotation_started_at`을 함께 본다.
- 키 버전 정보가 DB에 없으므로, 회전 기록이 없으면 나중에 정확한 비교 근거가 약해진다.

### 월 1회 점검 체크리스트

- `APP_AUDIT_REQUEST_IP_HMAC_SECRET`가 설정돼 있는지 확인
- prune job이 최근 24시간 안에 한 번 이상 실행됐는지 확인
- `changed_at`이 30일을 넘는 레코드에 `request_ip_hmac`가 남아 있지 않은지 샘플 확인
- 최근 키 회전 기록이 운영 기록에 남아 있는지 확인

## 2. Recovery Email SMTP 실메일 점검

### 목표

- 운영 환경에서 recovery email이 실제로 발송되고, 사용자가 받은 링크로 검증을 완료할 수 있는지 확인한다.

### 최근 확인 기록

- 최신 실메일 스모크 테스트 보고서: [2026-03-17-smtp-smoke-test.md](../reports/ops/2026-03-17-smtp-smoke-test.md)
- 이번 기록은 `smtp.naver.com` 기준 실메일 도착, preview/confirm, verified 반영까지 확인한 결과다.

### 사전 준비

- 발신 주소 또는 도메인이 SMTP/SES 제공자에서 검증돼 있어야 한다.
- SES를 쓰는 경우 샌드박스 여부와 production access 상태를 먼저 확인한다.
- `APP_PUBLIC_BASE_URL`이 실제 운영 도메인을 가리켜야 한다.
- 운영에서는 `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false`를 유지한다.

### 관련 설정

```powershell
$env:APP_PUBLIC_BASE_URL='https://your-domain.example'
$env:APP_RECOVERY_EMAIL_FROM='no-reply@your-domain.example'
$env:APP_RECOVERY_EMAIL_MAIL_ENABLED='true'
$env:APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED='false'
$env:MAIL_HOST='smtp.example.com'
$env:MAIL_PORT='587'
$env:MAIL_USERNAME='smtp-user'
$env:MAIL_PASSWORD='smtp-password'
```

### 1차 연결 점검

1. 운영 환경에 위 값을 반영한다.
2. 앱을 재기동한다.
3. 헬스체크와 로그인 화면이 정상인지 먼저 확인한다.
4. 테스트용 계정 또는 새 계정을 준비한다.

### 실메일 스모크 테스트

1. 회원가입 또는 설정의 recovery email 변경으로 실제 메일 발송을 유도한다.
2. 테스트 메일함에서 메일이 도착했는지 확인한다.
3. 제목이 목적에 맞는지 확인한다.
   - 회원가입: `[BootSync] 회원가입 복구 이메일 인증`
   - 변경: `[BootSync] 복구 이메일 변경 인증`
4. 메일 본문의 링크가 실제 운영 도메인인지 확인한다.
5. 링크를 열어 preview 화면이 뜨는지 확인한다.
6. 확인 버튼으로 최종 확정이 되는지 확인한다.
7. 완료 후 `/api/auth/session` 또는 설정 화면에서 verified 상태가 반영됐는지 확인한다.

자동화 보조:

- [Invoke-RecoveryEmailSmtpSmokeTest.ps1](../../scripts/ops/Invoke-RecoveryEmailSmtpSmokeTest.ps1)은 mail-enabled 상태로 이미 기동한 앱을 상대로 회원가입을 호출해 실제 메일 발송을 유도하고, `build/ops-smtp/reports` 아래에 확인용 markdown 초안을 생성한다.
- 예시:

```powershell
pwsh -File .\scripts\ops\Invoke-RecoveryEmailSmtpSmokeTest.ps1 `
  -BaseUrl 'http://localhost:18080' `
  -TargetEmail 'qa@example.com' `
  -FromAddress 'no-reply@example.com' `
  -MailHost 'smtp.example.com'
```

- 이 스크립트는 메일함 확인과 링크 confirm 자체까지 자동화하지는 않는다. 메일 도착, 링크 도메인, confirm 성공 여부는 수동 확인 후 보고서에 채운다.

### 재발송 점검

1. pending 인증 상태를 만든다.
2. `POST /api/settings/recovery-email/resend` 또는 `/app/settings` 재발송 버튼을 사용한다.
3. 새 메일이 도착하는지 확인한다.
4. 직후 재발송 시 cooldown 메시지가 의도대로 나오는지 확인한다.

### 실패 시 1차 확인 순서

1. `APP_RECOVERY_EMAIL_MAIL_ENABLED=true` 인지 확인
2. `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` 확인
3. 발신 주소가 제공자에서 검증된 identity인지 확인
4. `APP_PUBLIC_BASE_URL`이 올바른 도메인인지 확인
5. SMTP 제공자 로그 또는 SES 발송 로그에서 reject/bounce 여부 확인

### 롤백 기준

- 실메일 발송이 불안정하면 즉시 `APP_RECOVERY_EMAIL_MAIL_ENABLED=false`로 되돌린다.
- 운영에서 preview 링크까지 다시 열지는 않는다.
- 문제 해결 전까지 recovery email 관련 변경 공지를 사용자에게 안내한다.

## 3. 삭제 요청 등록부와 삭제 취소 runbook

### 목표

- 삭제 요청이 접수되면 계정 상태 전환, 세션 차단, 유예기간, 실제 purge까지 한 흐름으로 추적한다.
- 삭제 요청 취소는 `delete_due_at` 이전에만 수행하고, 취소 사유와 본인 확인 근거를 운영 기록에 남긴다.
- 백업 복원 후 삭제 계정이 되살아나는 일을 막기 위해 `삭제 요청 등록부`를 앱 외부에서 유지한다.

### 현재 구현 계약

- 사용자 self-service 삭제 요청 엔드포인트:
  - API: `POST /api/settings/account-deletion`
- 삭제 요청 조건:
  - 검증된 `recovery_email` 보유
  - 현재 비밀번호 일치
- 삭제 요청 성공 시:
  - `status = PENDING_DELETE`
  - `delete_requested_at = now`
  - `delete_due_at = now + 7일`
  - 현재 세션 즉시 로그아웃
- 보호 화면 요청마다 계정 상태를 재검사하고 `ACTIVE`가 아니면 세션을 차단한다.
- 취소는 `AccountDeletionService.cancelDeletion(username)`로만 가능하며 `delete_due_at` 이전의 `PENDING_DELETE` 계정만 허용한다.
- purge는 `AccountDeletionPurgeJob`이 켜져 있을 때 `status = PENDING_DELETE and delete_due_at <= now` 조건으로 처리한다.
- 운영 rehearsal이나 첫 수동 점검을 위해 `AccountDeletionPurgeRunner`를 일회성으로 실행할 수 있다.
- 운영자 보조 삭제 취소는 `AccountDeletionCancelRunner`로 일회성 실행할 수 있다.

### purge 관련 설정

```powershell
$env:APP_ACCOUNT_DELETION_PURGE_ENABLED='true'
$env:APP_ACCOUNT_DELETION_PURGE_CRON='0 15 3 * * *'
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED='false'
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ACTOR=''
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_REASON=''
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_CLOSE_CONTEXT_AFTER_RUN='true'
```

- `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED`
  기본값은 `false`다. 필요할 때만 잠깐 `true`로 켠다.
- `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ACTOR`
  수행자 식별값이다. 운영 기록과 같은 값을 사용한다.
- `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_REASON`
  실행 사유다. `ticket-123 purge rehearsal` 같은 형태를 권장한다.
- `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_CLOSE_CONTEXT_AFTER_RUN`
  기본값은 `true`다. 실행이 끝나면 앱 컨텍스트를 자동 종료한다.

### 삭제 요청 등록부 필수 필드

운영자는 앱 외부의 접근 통제된 문서 또는 티켓 시스템에 아래 값을 남긴다.

- `request_type`
  `account_deletion_request`, `account_deletion_cancel`, `password_reset_assist` 중 하나
- `ticket_id`
- `member_id`
- `username`
- `request_channel`
- `identity_verification_basis`
- `reviewed_by`
- `executed_by`
- `requested_at`
- `executed_at`
- `delete_requested_at`
- `delete_due_at`
- `outcome`
- `reason`
- `last_related_backup_expire_at`

### 접수 채널 원칙

- 삭제 요청과 삭제 취소는 검증된 recovery email 회신이 가능한 운영 메일함 또는 접근 통제된 운영 티켓에서만 받는다.
- 일반 채팅, 메신저 DM, 전화만으로는 원격 고위험 요청을 확정하지 않는다.
- 검증된 recovery email이 없는 계정은 원격 삭제 요청/취소 대상으로 취급하지 않는다.

### self-service 삭제 요청 후 운영 확인 절차

1. 사용자에게 `/settings` 또는 `/app/settings`에서 직접 삭제 요청하도록 안내한다.
2. 요청이 성공하면 아래 값을 확인한다.
   - `status = PENDING_DELETE`
   - `delete_requested_at` 기록 여부
   - `delete_due_at = delete_requested_at + 7일`
3. 기존 세션이 더 이상 보호 화면에 접근되지 않는지 확인한다.
4. 삭제 요청 등록부에 계정 정보와 예정 삭제 시각을 기록한다.
5. 마지막 관련 백업 만료 시점까지 등록부를 보존한다.

### 운영자 보조 삭제 취소 절차

1. 요청 채널이 운영 메일함 또는 보안 티켓인지 확인한다.
2. 검증된 recovery email 통제 증명이 있는지 확인한다.
3. 계정 상태가 `PENDING_DELETE`이고 `delete_due_at > now` 인지 확인한다.
4. 삭제 요청 등록부에 취소 요청 시각, 검토자, 본인 확인 근거, 취소 사유를 먼저 남긴다.
5. 사전에 승인된 통제된 유지보수 경로가 준비돼 있을 때만 `cancelDeletion(username)`을 수행한다.
   현재 코드베이스에는 공개 운영자 UI가 없으므로, 임시 운영 콘솔, 테스트 코드 재사용, 즉흥 SQL로 처리하지 않는다.
   대신 전용 `AccountDeletionCancelRunner` 같은 maintenance path만 사용한다.
6. 취소 후 아래 값을 확인한다.
   - `status = ACTIVE`
   - `delete_requested_at = null`
   - `delete_due_at = null`
7. 사용자의 기존 세션은 다시 살리지 않고, 새 로그인만 안내한다.
8. 등록부에 `executed_at`, `executed_by`, `outcome`를 기록한다.

### 삭제 취소 maintenance runner 실행 예시

삭제 요청 취소는 아래 설정을 일시적으로 주입한 뒤 앱을 한 번 실행하는 방식으로 처리한다.
기본값은 `close-context-after-run=true`라서 성공 후 컨텍스트가 자동 종료된다.

```powershell
$env:APP_OPERATIONS_DELETION_CANCEL_ENABLED='true'
$env:APP_OPERATIONS_DELETION_CANCEL_USERNAME='target-user'
$env:APP_OPERATIONS_DELETION_CANCEL_ACTOR='ops-admin'
$env:APP_OPERATIONS_DELETION_CANCEL_REASON='ticket-123 deletion cancel'

.\gradlew.bat bootRun --args="--spring.profiles.active=prod"
```

- 이 runner는 `delete_due_at` 이전의 `PENDING_DELETE` 계정만 복구한다.
- 실행 후 로그에는 `BootSync operator account deletion cancel completed: ...` 형식으로 결과가 남는다.
- 실행 직후 등록부에 `executed_at`, `executed_by`, `outcome`를 갱신한다.

### purge 운영 활성화 체크리스트

- `APP_ACCOUNT_DELETION_PURGE_ENABLED=true` 인지 확인
- cron이 KST 기준 운영 의도와 맞는지 확인
- 최소 1회 스테이징 또는 로컬에서 purge one-shot rehearsal 기록 확보
- 삭제 취소용 maintenance path와 별개로, purge 자체는 운영 배치로 자동 실행되는지 확인
- 운영 첫 활성화 후 다음 항목을 기록
  - 활성화 시각
  - 수행자
  - 첫 실행 시각
  - 처리 건수
  - 오류 여부

### purge one-shot rehearsal 절차

운영 첫 활성화 전에, 동일 코드 경로로 one-shot purge를 한 번 실행해 기록을 남긴다.

```powershell
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED='true'
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ACTOR='ops-admin'
$env:APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_REASON='ticket-123 purge rehearsal'

.\gradlew.bat bootRun --args="--spring.profiles.active=prod"
```

- 기본값은 `close-context-after-run=true`라서 purge가 끝나면 앱이 자동 종료된다.
- rehearsal 대상은 실제 운영 계정이 아니라, 스테이징 또는 로컬의 `PENDING_DELETE` 테스트 계정으로 준비한다.
- 로그에는 `BootSync one-shot account deletion purge completed: purgedCount=...` 형식으로 처리 건수가 남는다.
- rehearsal 직후 아래 항목을 운영 기록에 남긴다.
  - 실행 시각
  - 수행자
  - 실행 사유
  - 대상 환경
  - `purgedCount`
  - 오류 여부
- 로컬 rehearsal 예시는 [2026-03-15-ops-rehearsal.md](../reports/ops/2026-03-15-ops-rehearsal.md)에 남겼다.

### 운영 첫 활성화 후 기록 원칙

- 스케줄 기반 운영 purge는 `BootSync scheduled account deletion purge completed: purgedCount=...` 로그를 기준으로 첫 실행 기록을 남긴다.
- 첫 실행 기록에는 최소 아래 값을 남긴다.
  - `enabled_at`
  - `first_scheduled_run_at`
  - `purgedCount`
  - `reviewed_by`
  - `rollback_needed`

### purge 기본 순서

코드 기준 purge 순서는 아래와 같다.

1. `snippet_tag`
2. `snippet`
3. `tag`
4. `attendance_record`
5. `recovery_email_verification_token`
6. `attendance_audit_log` 비식별화
7. `member`

현재 purge는 일부 데이터가 이미 부분 삭제된 상태에서도 재실행 가능한 멱등 구조다.

## 4. Recovery Email 기반 운영자 보조 계정 복구

### 목표

- 사용자가 비밀번호를 잊었을 때, 검증된 recovery email 통제 증명을 기준으로 운영자가 최소 범위로 복구를 돕는다.
- 원격 비밀번호 초기화는 자동 self-service가 아니라 운영자 보조 절차로 처리한다.

### 현재 제품 계약

- 공개 사용자 기능에는 self-service 비밀번호 재설정 링크가 없다.
- 원격 보조 대상은 `recovery_email_verified_at`가 있는 계정만 허용한다.
- 검증된 recovery email이 없는 계정은 원격 비밀번호 초기화 대상이 아니다.
- `PENDING_DELETE`, `DISABLED` 계정은 일반 복구 절차로 바로 진행하지 않고 상태를 먼저 검토한다.
- 전용 maintenance runner가 현재 코드베이스에 포함돼 있으며, 공개 HTTP 엔드포인트 없이 일회성 실행으로 임시 비밀번호를 교체할 수 있다.

### 접수 채널

- 검증된 recovery email 회신이 가능한 운영 메일함
- 접근 통제된 운영 티켓

위 두 채널 외 요청은 접수하지 않는다.

### 본인 확인 최소 절차

1. 요청을 운영 티켓으로 등록한다.
2. `username` 기준으로 계정을 찾고 아래를 확인한다.
   - `status = ACTIVE`
   - `recovery_email` 존재
   - `recovery_email_verified_at` 존재
3. 등록된 recovery email로 1회성 확인 메시지를 보낸다.
4. 사용자가 그 메일함을 실제로 통제하고 있다는 증거를 확인한다.
5. 운영 기록에 `identity_verification_basis`를 남긴다.

### 실행 원칙

- 현재 코드베이스에는 공개 운영자 UI는 없지만, 전용 `OperatorPasswordResetRunner`가 있다.
- 원격 비밀번호 초기화는 문서화된 보안 티켓과 이 maintenance runner 같은 통제된 유지보수 절차가 함께 준비된 환경에서만 수행한다.
- 일반 DB 클라이언트에서 즉흥적으로 수정하거나, 채팅만 믿고 처리하지 않는다.
- 임시 비밀번호를 발급했다면 사용자가 첫 로그인 후 즉시 `/settings` 또는 `/app/settings`에서 비밀번호를 변경하도록 안내한다.
- 비밀번호가 바뀌면 기존 세션은 다음 보호 요청에서 자동 로그아웃된다.

### maintenance runner 실행 예시

운영자 보조 비밀번호 초기화는 아래 설정을 일시적으로 주입한 뒤 앱을 한 번 실행하는 방식으로 처리한다.
기본값은 `close-context-after-run=true`라서 성공 후 컨텍스트가 자동 종료된다.

```powershell
$env:APP_OPERATIONS_PASSWORD_RESET_ENABLED='true'
$env:APP_OPERATIONS_PASSWORD_RESET_USERNAME='target-user'
$env:APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD_FILE='C:\secure\bootsync-temp-password.txt'
$env:APP_OPERATIONS_PASSWORD_RESET_ACTOR='ops-admin'
$env:APP_OPERATIONS_PASSWORD_RESET_REASON='ticket-123 password reset assist'

.\gradlew.bat bootRun --args="--spring.profiles.active=prod"
```

- `APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD_FILE` 방식을 권장한다.
- inline 값 `APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD`도 지원하지만, 쉘 기록 노출 위험이 더 크다.
- 실행 후에는 임시 비밀번호 파일과 관련 환경변수를 즉시 정리한다.

### 운영 기록 필수 필드

- `request_type = password_reset_assist`
- `ticket_id`
- `member_id`
- `username`
- `request_channel`
- `identity_verification_basis`
- `reviewed_by`
- `executed_by`
- `executed_at`
- `outcome`
- `reason`

### 현재 남아 있는 운영 갭

- maintenance runner는 구현돼 있고, 로컬 첫 rehearsal 기록은 [2026-03-15-ops-rehearsal.md](../reports/ops/2026-03-15-ops-rehearsal.md)에 남겼다.
- 공개 출시 전에는 실제 운영 또는 prod-like 환경 기준으로도 결과를 운영 기록에 남겨야 한다.

## 5. 백업 / 복원 runbook

### 현재 상태

- [Invoke-MySqlBackupToS3.ps1](../../scripts/ops/Invoke-MySqlBackupToS3.ps1), [Invoke-MySqlRestoreFromS3.ps1](../../scripts/ops/Invoke-MySqlRestoreFromS3.ps1)로 백업/복원 스크립트가 추가됐다.
- 로컬 backup/restore rehearsal 기록은 [2026-03-15-backup-restore-rehearsal.md](../reports/ops/2026-03-15-backup-restore-rehearsal.md)에 남겼다.
- 현재 스크립트 구현은 `docker exec/cp`로 MySQL 컨테이너를 기준으로 동작하므로, 최종 운영 DB를 `RDS`로 쓸 때는 실행 위치나 스크립트 입력값을 RDS 기준으로 보완해야 한다.
- 공개 출시 전에는 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치, prod-like 복원 리허설과 `RTO 8시간` 실측이 별도로 필요하다.

### 목표 기준

- 백업 주기: 하루 1회
- 보존 정책: 최근 7일 일일 백업 + 최근 4주 주간 백업
- 복구 목표: `RPO 24시간`, `RTO 8시간`
- 백업 저장소: 접근 통제된 별도 S3 버킷

### 사전 준비

- 운영 인프라에 AWS CLI 접근이 준비돼 있어야 한다.
- `BACKUP_S3_BUCKET`, `AWS_REGION` 같은 운영 변수는 앱 바깥의 cron/script에서 사용한다.
- 백업 버킷은 `Block Public Access`, 기본 암호화, 최소 권한 IAM 정책을 사용한다.
- 복원 읽기 권한과 백업 쓰기 권한은 가능하면 분리한다.

### 자동화 스크립트 사용

로컬 검증이나 운영 스케줄러에서는 아래 스크립트를 우선 사용한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\ops\Invoke-MySqlBackupToS3.ps1 -SkipUpload
```

- dump, manifest, markdown report를 `build\ops-backup` 아래에 남긴다.
- `-SkipUpload`는 로컬 dump/restore rehearsal이나 배포 전 smoke test에 사용한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\ops\Invoke-MySqlBackupToS3.ps1
```

- 운영 환경에서는 `BACKUP_S3_BUCKET`, `AWS_REGION`, 선택적으로 `AWS_PROFILE`을 준비한다.
- 기본 업로드 경로는 `daily/`이고, 일요일 또는 `-ForceWeekly` 지정 시 `weekly/`도 함께 기록한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\ops\Invoke-MySqlRestoreFromS3.ps1 -SourceFile .\build\ops-backup\bootsync-YYYYMMDD-HHMMSS.sql
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\ops\Invoke-MySqlRestoreFromS3.ps1 -S3Key daily/bootsync-YYYYMMDD-HHMMSS.sql
```

- 복원 로그는 `build\ops-restore\logs` 아래에 남긴다.
- 실제 서비스 reopen 전에는 아래 scrub 순서와 smoke test를 반드시 이어서 수행한다.

### 수동 백업 예시

```powershell
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDir = 'C:\bootsync-backups'
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

docker exec bootsync-mysql sh -lc 'exec mysqldump -ubootsync -p"$MYSQL_PASSWORD" --databases bootsync --single-transaction --routines --triggers --set-gtid-purged=OFF --default-character-set=utf8mb4' > "$backupDir\\bootsync-$timestamp.sql"

aws s3 cp "$backupDir\\bootsync-$timestamp.sql" "s3://$env:BACKUP_S3_BUCKET/daily/bootsync-$timestamp.sql" --region $env:AWS_REGION
```

### 백업 성공 후 확인 항목

- 덤프 파일 크기가 비정상적으로 작지 않은지 확인
- manifest의 SHA-256과 report 파일이 함께 생성됐는지 확인
- S3 업로드 성공 여부 확인
- 백업 수행 시각과 수행자를 운영 기록에 남김
- 실패 시 다음 백업 시점 전에 운영자가 감지할 수 있도록 티켓 또는 알람으로 남김

### 복원 전 원칙

- 복원 중에는 공개 트래픽을 열지 않는다.
- 가장 최근 성공 백업과 삭제 요청 등록부를 먼저 확보한다.
- 복원 대상 DB는 깨끗한 상태로 준비한다.
- 복원 후에는 `삭제 요청 등록부`를 기준으로 삭제 계정 scrub가 끝나기 전까지 서비스 재오픈 금지다.

### 수동 복원 예시

```powershell
$restoreFile = 'C:\bootsync-backups\bootsync-restore.sql'

aws s3 cp "s3://$env:BACKUP_S3_BUCKET/daily/bootsync-restore.sql" $restoreFile --region $env:AWS_REGION
docker cp $restoreFile bootsync-mysql:/tmp/bootsync-restore.sql
docker exec bootsync-mysql sh -lc 'exec mysql -ubootsync -p"$MYSQL_PASSWORD" bootsync < /tmp/bootsync-restore.sql'
```

### 복원 후 삭제 계정 scrub 순서

중요: 복원본에는 이미 삭제됐어야 할 계정 데이터가 남아 있을 수 있다. 아래 순서는 현재 `AccountDeletionPurgeService`와 동일한 기준을 반영한다.

1. 삭제 요청 등록부에서 아직 살아 있으면 안 되는 계정을 확인한다.
2. 계정별로 `member_id`, `username`, 삭제 상태를 대조한다.
3. 수동 scrub가 필요하면 아래 순서로 동일 계정 데이터를 제거하거나 비식별화한다.

```sql
DELETE FROM snippet_tag WHERE member_id = ?;
DELETE FROM snippet WHERE member_id = ?;
DELETE FROM tag WHERE member_id = ?;
DELETE FROM attendance_record WHERE member_id = ?;
DELETE FROM recovery_email_verification_token WHERE member_id = ?;
UPDATE attendance_audit_log
   SET attendance_record_id = NULL,
       member_id = NULL,
       changed_by_member_id = NULL,
       request_ip_hmac = NULL
 WHERE member_id = ?
    OR changed_by_member_id = ?;
DELETE FROM member WHERE id = ?;
```

4. 삭제 요청 등록부에 scrub 수행 시각, 수행자, 대상 `member_id`를 남긴다.
5. scrub이 모두 끝난 뒤에만 앱을 다시 연다.
- 로컬 rehearsal 예시는 [2026-03-15-ops-rehearsal.md](../reports/ops/2026-03-15-ops-rehearsal.md)에 남겼다.

### 복원 후 스모크 테스트

복원 직후 아래 네 흐름을 최소 확인한다.

1. 로그인
2. 대시보드 조회
3. 출결 저장
4. 스니펫 조회

삭제 요청 계정이 다시 로그인되지 않는지도 함께 확인한다.

### 월 1회 점검 체크리스트

- 최근 24시간 내 성공 백업 존재 여부
- 최근 1회 이상 복원 리허설 또는 덤프 무결성 확인 기록
- 삭제 요청 등록부가 최신 상태인지 여부
- 백업 버킷 lifecycle과 암호화 설정 유지 여부

## 6. 운영 기록에 남길 항목

다음 항목은 앱 외부의 접근 통제된 운영 문서 또는 티켓 시스템에 남긴다.

- `request_ip_hmac` 키 생성 / 교체 / 폐기 기록
- SMTP 설정 변경 시점
- 실메일 점검 수행 시각과 수행자
- 삭제 요청 / 삭제 취소 / 비밀번호 초기화 보조 기록
- 테스트 계정 또는 테스트 대상 이메일
- 백업 성공 / 실패 기록
- 복원 리허설 수행 시각, 수행자, 결과
- 장애 / rollback 여부

## 7. 현재 남은 후속 과제

- 운영 SMTP 실메일 스모크 테스트 수행
- purge 스케줄 운영 첫 실행 기록 확보
- AWS 실제 배포(`ECR -> RDS -> EC2/k3s`) 진행
- 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치
- prod-like 복원 리허설 수행과 `RTO 8시간` 실측
- 개인정보 처리방침 / 이용약관 / 삭제·복구 정책 확정과 공개 운영 값 반영
