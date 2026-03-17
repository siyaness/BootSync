# BootSync 운영 증적 템플릿

최종 수정일: 2026-03-16

이 문서는 아직 실제 운영 증적이 없는 항목을 빠르게 기록할 수 있도록 만든 템플릿 모음입니다. README, PROJECT_PLAN, SPEC_TRACKER에서 공통으로 추적하는 `운영 SMTP 실메일 스모크 테스트`, `purge 스케줄 운영 첫 실행 기록`, `운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치`, `prod-like 복원 리허설과 RTO 8시간 실측`을 같은 기준으로 남기기 위한 템플릿입니다. 각 항목은 실행 후 새 markdown 보고서로 복사해 채우는 것을 권장합니다.

참고:

- SMTP 실메일 스모크 테스트는 [Invoke-RecoveryEmailSmtpSmokeTest.ps1](../../scripts/ops/Invoke-RecoveryEmailSmtpSmokeTest.ps1)로 회원가입 발송 트리거와 초안 보고서 생성을 자동화할 수 있습니다.
- 최신 예시는 [2026-03-17-smtp-smoke-test.md](../reports/ops/2026-03-17-smtp-smoke-test.md)에서 확인할 수 있습니다.

## 1. SMTP 실메일 스모크 테스트 템플릿

파일명 예시:

- `docs/reports/ops/2026-03-20-smtp-smoke-test.md`

템플릿:

```md
# SMTP 실메일 스모크 테스트

- 실행 시각:
- 실행 환경:
- 수행자:
- 대상 도메인:
- 테스트 계정:

## 설정 확인

- APP_PUBLIC_BASE_URL:
- APP_RECOVERY_EMAIL_FROM:
- APP_RECOVERY_EMAIL_MAIL_ENABLED:
- APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED:
- MAIL_HOST:
- MAIL_PORT:

## 수행 절차

1. 회원가입 또는 복구 이메일 변경으로 발송 유도
2. 메일 수신 확인
3. 링크 도메인 확인
4. 링크 진입 후 confirm 완료
5. verified 상태 반영 확인
6. resend cooldown 확인

## 결과

- 메일 도착 여부:
- 링크 도메인 일치 여부:
- confirm 성공 여부:
- resend 성공 여부:
- cooldown 메시지 정상 여부:

## 증적

- 첨부 스크린샷:
- 메일 원문 또는 제목:
- 운영 로그:

## 이슈 / 후속 작업
```

## 2. purge 스케줄 첫 실행 기록 템플릿

파일명 예시:

- `docs/reports/ops/2026-03-21-purge-first-scheduled-run.md`

템플릿:

```md
# purge 스케줄 첫 실행 기록

- 실행 시각:
- 실행 환경:
- 수행자:
- cron:

## 사전 조건

- APP_ACCOUNT_DELETION_PURGE_ENABLED=true
- APP_ACCOUNT_DELETION_PURGE_CRON 확인
- 삭제 요청 등록부 최신화 여부 확인

## 결과

- 이번 실행 대상 건수:
- 실제 purge 처리 건수:
- 처리 후 예외 발생 여부:
- 로그 위치:

## 검증

- 대상 계정 row 삭제 확인:
- 관련 출결 / 학습 노트 삭제 확인:
- audit 비식별화 확인:
- 등록부 업데이트 완료 여부:

## 증적

- 애플리케이션 로그:
- DB 확인 캡처:

## 이슈 / 후속 작업
```

## 3. S3 업로드 1회 기록 템플릿

파일명 예시:

- `docs/reports/ops/2026-03-22-s3-backup-upload.md`

템플릿:

```md
# S3 백업 업로드 기록

- 실행 시각:
- 실행 환경:
- 수행자:
- AWS 리전:
- S3 버킷:
- 실행 명령:

## 결과

- dump 파일명:
- manifest 생성 여부:
- report 생성 여부:
- S3 daily 업로드 성공 여부:
- S3 weekly 업로드 성공 여부:

## 증적

- S3 object key:
- object size:
- SHA-256:
- 콘솔/CLI 로그:

## 이슈 / 후속 작업
```

## 4. prod-like 복원 및 RTO 기록 템플릿

파일명 예시:

- `docs/reports/ops/2026-03-22-prod-like-restore.md`

템플릿:

```md
# prod-like 복원 및 RTO 기록

- 시작 시각:
- 종료 시각:
- 총 소요 시간:
- 수행자:
- 소스 백업:
- 복원 대상 환경:

## 복원 절차

1. 백업 선택
2. 대상 DB 준비
3. 복원 실행
4. row count 확인
5. 앱 연결 확인
6. 기본 smoke test 수행

## 결과

- member row count:
- attendance_record row count:
- snippet row count:
- 로그인 확인:
- 출결 화면 확인:
- 학습 노트 화면 확인:

## RTO 판단

- 목표 RTO:
- 실제 소요:
- 목표 충족 여부:

## 증적

- 복원 로그:
- smoke test 캡처:

## 이슈 / 후속 작업
```
