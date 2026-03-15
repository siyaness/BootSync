# BootSync Ops Rehearsal Report

기준 시점: 2026-03-15 (Asia/Seoul)

이 문서는 로컬 환경에서 수행한 Phase 2 운영 rehearsal 3종의 실행 기록을 남긴다.
이번 기록은 `local` 프로필과 Docker Compose MySQL 기준이며, 실제 운영 첫 실행 기록이나 SMTP/S3 증적을 대체하지 않는다.

## 1. 실행 환경

- 앱 프로필: `local`
- DB: `docker compose up -d mysql`
- 실행 스크립트: [Invoke-LocalOpsRehearsals.ps1](/C:/B_Recheck/scripts/ops/Invoke-LocalOpsRehearsals.ps1)
- 생성 리포트: [local-ops-rehearsal-20260315-023821.md](/C:/B_Recheck/build/ops-rehearsal/local-ops-rehearsal-20260315-023821.md)
- 로그:
  - [seed-20260315-023821.log](/C:/B_Recheck/build/ops-rehearsal/logs/seed-20260315-023821.log)
  - [password-reset-20260315-023821.log](/C:/B_Recheck/build/ops-rehearsal/logs/password-reset-20260315-023821.log)
  - [purge-20260315-023821.log](/C:/B_Recheck/build/ops-rehearsal/logs/purge-20260315-023821.log)
  - [scrub-rehearsal-20260315-023821.sql](/C:/B_Recheck/build/ops-rehearsal/scrub-rehearsal-20260315-023821.sql)

## 2. Seed 결과

- 실행 시각: `2026-03-15 02:38:28 +09:00`
- 준비 계정:
  - `ops_reset_target` / member id `2`
  - `ops_purge_due_target` / member id `3`
  - `ops_scrub_target` / member id `4`
- seed runner 로그:
  `BootSync ops rehearsal seed completed: resetUsername=ops_reset_target, resetMemberId=2, purgeUsername=ops_purge_due_target, purgeMemberId=3, scrubUsername=ops_scrub_target, scrubMemberId=4`

## 3. 운영자 보조 비밀번호 초기화 rehearsal

- 대상 계정: `ops_reset_target`
- `updated_at` 변경:
  - before: `2026-03-15 02:33:28.533007`
  - after: `2026-03-15 02:38:35.685551`
- 임시 비밀번호 파일:
  [ops-reset-temp-password-20260315-023821.txt](/C:/B_Recheck/build/ops-rehearsal/ops-reset-temp-password-20260315-023821.txt)
- runner 로그:
  `BootSync operator password reset completed: memberId=2, username=ops_reset_target, recoveryEmail=op**************@ex*********, actor=local-ops-admin, reason=local password reset rehearsal`

## 4. purge one-shot rehearsal

- 대상 계정: `ops_purge_due_target` / member id `3`
- 실행 전 카운트:
  - `member = 1`
  - `attendance_record = 1`
  - `attendance_audit_log_active = 1`
  - `snippet = 1`
  - `snippet_tag = 1`
  - `tag = 1`
  - `recovery_email_verification_token = 1`
- 실행 후 카운트:
  - 모든 본 데이터 카운트 `0`
  - audit row 상태: `NULL/NULL/NULL`
- runner 로그:
  `BootSync one-shot account deletion purge completed: purgedCount=1, actor=local-ops-admin, reason=local purge rehearsal`

## 5. 복원 후 scrub rehearsal

- 대상 계정: `ops_scrub_target` / member id `4`
- 실행 전 카운트:
  - `member = 1`
  - `attendance_record = 1`
  - `attendance_audit_log_active = 1`
  - `snippet = 1`
  - `snippet_tag = 1`
  - `tag = 1`
  - `recovery_email_verification_token = 1`
- 실행 후 카운트:
  - 모든 본 데이터 카운트 `0`
  - audit row 상태: `NULL/NULL/NULL`
- 적용 SQL은 [scrub-rehearsal-20260315-023821.sql](/C:/B_Recheck/build/ops-rehearsal/scrub-rehearsal-20260315-023821.sql)에 남겼다.

## 6. 이번 기록으로 닫힌 것

- 로컬 `purge one-shot rehearsal` 증적
- 운영자 보조 비밀번호 초기화 첫 rehearsal 증적
- 삭제 요청 등록부 기준 `복원 후 scrub` 로컬 rehearsal 증적

## 7. 아직 남은 것

- 실제 운영 또는 prod-like 환경에서 purge 스케줄 첫 실행 기록 확보
- 운영 SMTP 실메일 smoke test
- S3 백업 자동화와 복원 리허설 실측, `RTO 8시간` 검증
- 개인정보 처리방침 / 이용약관 확정
