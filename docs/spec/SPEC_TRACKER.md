# BootSync Spec Tracker

상태 값:

- `DONE`: 구현과 테스트가 모두 확인된 상태
- `IN_PROGRESS`: 일부 구현됐지만 명세 기준으로 아직 남은 갭이 있는 상태
- `TODO`: 아직 본격 구현 전
- `OPS`: 코드보다 운영 준비가 핵심인 상태

## 1. 인증 / 세션 / 계정

| 항목 | 상태 | 메모 |
|---|---|---|
| 회원가입 후 자동 로그인 | DONE | 가입 직후 대시보드 이동 |
| `ACTIVE` 계정만 로그인 허용 | DONE | 비활성 상태는 로그인 차단 |
| 보호 요청 시 계정 상태 재검사 | DONE | `PENDING_DELETE`/`DISABLED` 세션 종료 |
| 원래 요청한 보호 화면 복귀 | DONE | saved request 기반 |
| custom principal에 `memberId`, `username`, `displayName` 포함 | DONE | `BootSyncPrincipal` 사용 |
| 세션 고정 공격 방지 | DONE | session fixation migrate 설정 |
| rate limit용 클라이언트 IP 신뢰 경계 | DONE | 기본은 `remoteAddr`, 운영에서만 forwarded header opt-in |

## 2. Recovery Email

| 항목 | 상태 | 메모 |
|---|---|---|
| signup verification token 발급/검증 | DONE | GET preview, POST confirm |
| change verification token 발급/검증 | DONE | settings change flow 연결 |
| 계정별 최신 pending verification target만 활성 | DONE | 새 발급 또는 confirm 성공 시 기존 미사용 token 일괄 무효화 |
| 검증 전 `member.recovery_email` 미승격 | DONE | confirm 시점에만 반영 |
| 기존 verified email 유지 후 새 이메일 검증 | DONE | change confirm 전까지 기존 값 유지 |
| resend 단일 엔드포인트 | DONE | `/api/settings/recovery-email/resend` |
| 계정/IP rate limit + resend cooldown | DONE | 계정 5회/1시간, IP 10회/1시간, resend cooldown 1분 회귀 테스트 포함 |
| 개발용 preview 링크 노출 경계 | DONE | 기본 비활성, `local`/`test`에서만 활성 |
| 운영 SMTP 실발송 점검 | OPS | runbook은 추가됨, 실제 운영 환경 실메일 smoke test만 남음 |
| 운영자 보조 계정 복구 절차 | DONE | runbook + password reset maintenance runner + 로컬 rehearsal report + 기존 세션 차단 회귀 테스트 반영 |

## 3. Dashboard

| 항목 | 상태 | 메모 |
|---|---|---|
| 로그인 사용자 기준 대시보드 | DONE | 출결/최근 스니펫 반영 |
| 단위기간 장려금 요약 | DONE | React `/app` 화면이 서버 `allowanceSummary`를 사용해 `1일 지급액 × 지급 반영 일수(20일 상한)` 기준으로 현재 단위기간 예상 장려금을 표시 |
| 수강 리스크 카드 | DONE | 대시보드는 핵심 4개 지표만 요약, 상세 내용은 `과정 현황` 페이지로 분리 |
| 명세 안내 문구 점검 | IN_PROGRESS | 카피 최종 점검 필요 |

## 4. Attendance

| 항목 | 상태 | 메모 |
|---|---|---|
| 월별 조회 | DONE | `yearMonth` 이동 가능 |
| 날짜별 등록/수정 | DONE | 업서트 동작, `/app` UI도 명시 저장 기준으로 주말을 자동 차단하지 않음 |
| 빈 수업일 일괄 출석 | DONE | 저장된 `내 과정 정보`가 있으면 과정 시작일부터 오늘까지 비어 있는 수업일만 `출석`으로 채우고, 기존 기록/휴강일/비수업일/미래 날짜는 제외 |
| 미래 날짜 차단 | DONE | 폼/API/서비스 레벨 검증 및 회귀 테스트 있음 |
| 동일 날짜 중복 방지 | DONE | DB + 업서트 처리 |
| 삭제 | DONE | 본인 기록만 delete 가능 |
| PATCH/DELETE 표면 정리 | DONE | API 중심 `PUT/DELETE` 표면으로 정리, 레거시 폼 화면 제거 |
| 감사 로그 | DONE | create/update/delete, 선택적 `request_ip_hmac`, 30일 자동 비움, 키 회전 runbook 반영 |

## 5. Snippet

| 항목 | 상태 | 메모 |
|---|---|---|
| 목록/검색/단일 태그 필터 | DONE | 사용자 스코프 적용 |
| 상세/수정 소유권 검사 | DONE | 타인 리소스 404 |
| 작성/수정 | DONE | 태그 포함 |
| 삭제 | DONE | 상세 화면 delete flow + orphan tag cleanup |
| secret warning token 흐름 | DONE | 1차 차단, 2차 승인 저장 |
| 안전한 Markdown 렌더링 | DONE | commonmark + sanitizer 적용 |

## 6. Settings / Course Status

| 항목 | 상태 | 메모 |
|---|---|---|
| 과정 현황 상세 페이지 | DONE | 현재까지 출석률/남은 수업일/리스크 상세 + 내 과정 정보 확인/수정, `1일 지급액`과 `지급 상한 일수`로 개인 장려금 규칙을 관리 |
| recovery email 상태 표시 | DONE | verified/pending 표시 |
| recovery email 변경 | DONE | 현재 비밀번호 확인 포함 |
| 비밀번호 변경 | DONE | 현재 비밀번호 확인, 세션 principal 즉시 갱신 |
| 기본 프로필 수정 | DONE | `display_name` trim 저장, 세션 principal 즉시 갱신 |

## 7. 계정 생명주기 / 개인정보

| 항목 | 상태 | 메모 |
|---|---|---|
| `PENDING_DELETE` 상태 반영 | DONE | 삭제 요청 시 즉시 상태 전환, 세션 종료 |
| 삭제 요청 등록/취소 절차 | DONE | 설정 요청 + 취소 서비스 + 삭제 요청 등록부/취소 runbook + deletion cancel maintenance runner |
| purge 책임 구현 | DONE | 전용 purge 서비스 + 선택적 스케줄 잡 + one-shot rehearsal runner + 멱등 테스트 |
| 개인정보/운영 문서 | IN_PROGRESS | 핵심 runbook + 로컬 ops/backup rehearsal report + 정책 문서 초안은 추가됨, 실제 운영 주체 값 확정과 실환경 증적은 남음 |

## 8. 운영 / 출시 준비

| 항목 | 상태 | 메모 |
|---|---|---|
| AWS 배포 | IN_PROGRESS | 제품 기능 범위와 별도로 운영 준비용 체크리스트와 `k8s/k8s-bootsync`, `k8s/k8s-monitoring`, `k8s/k8s-argocd` 초안 매니페스트는 추가됨, 실제 ECR/RDS/EC2+k3s 배포 실행은 남음 |
| S3 기반 일일 DB 백업 | OPS | backup/restore script와 로컬 rehearsal report는 추가됨, 실제 S3 업로드/AWS 스케줄/RTO 실측은 남음 |
| 헬스체크 | DONE | actuator health 공개 |
| 모니터링/로그 전략 | IN_PROGRESS | Prometheus/Grafana/node-exporter YAML, scrape token 기반 `/actuator/prometheus` 보호, app/monitoring secret 템플릿은 추가됨. 실제 운영 설치/대시보드/알람 기준은 남음 |
| README 최신화 | DONE | 현재 코드 상태 반영됨 |
