# BootSync 작업 기록

기준 시점: 2026-03-16

이 문서는 최근 진행한 프론트 전환, UX 개선, 버그 수정, 검증 이력을 한 곳에 남기기 위한 작업 기록이다.

## 1. 프론트엔드 전환

- 새 React 프론트를 [frontend](../../frontend)에서 별도 관리하고, 빌드 결과물만 Spring 앱의 `/app` 경로에 붙이도록 정리했다.
- 루트(`/`) 접속 시 현재 세션 상태에 따라 `/app/login` 또는 `/app/dashboard`로 이동하도록 연결했다.
- Gradle이 `frontend/dist`를 `build/generated-resources/frontend/static/app`으로 복사해 백엔드 코드와 프론트 소스가 직접 섞이지 않도록 구성했다.
- 초기 blank screen 문제는 React Router 기준 경로 처리와 정적 자산 반영 흐름을 정리해 해결했다.

## 2. 네비게이션 및 용어 정리

- 왼쪽 사이드바의 `BootSync` 로고를 누르면 대시보드로 돌아가도록 수정했다.
- 사이드바에 `훈련장려금` 메뉴를 추가했다.
- 사이드바 하단 계정 영역을 누르면 `/app/settings`로 이동하도록 연결했다.
- 사용자에게 보이는 `스니펫` 용어는 더 직관적인 `학습 노트`로 정리했다.

## 3. 복구 이메일 흐름 정리

- 새 복구 이메일 인증 흐름을 React 프론트 안에서 처리하도록 `/app/verify-email` 페이지를 연결했다.
- 새로 발급되는 인증 링크는 `/app/verify-email?purpose=...&token=...` 형식으로 이동한다.
- 인증 화면은 preview 조회와 confirm 요청을 실제 API로 처리하도록 정리했다.
- pending 인증 대상이 없으면 재발송 성공 메시지가 뜨지 않도록 수정했다.
- 재발송 cooldown은 5분에서 1분으로 줄였다.
- 설정/대시보드에서 현재 인증 대기 상태를 읽어오도록 연결했다.

## 4. 출결 관리 UX 개선

- 데스크톱 우선 흐름으로 `오늘 빠른 입력` 카드를 추가했다.
- 달력 날짜를 누르면 오른쪽 빠른 수정 패널에서 바로 `출석`, `지각`, `조퇴`, `결석`을 저장할 수 있게 바꿨다.
- 메모는 별도 저장 흐름으로 유지해 상태 입력과 분리했다.
- 현재 상태, 안내 문구, 삭제 영역의 자리를 고정해 저장 전후 레이아웃이 흔들리지 않도록 보정했다.

## 5. 버그 수정 이력

### 학습 노트 태그 입력

- 태그를 입력창에만 적고 바로 저장하면 배열에 반영되지 않던 문제를 수정했다.
- `Enter`, `,`, `Tab`으로 태그를 확정할 수 있게 했고, 저장 시 아직 확정하지 않은 입력도 자동 반영되도록 바꿨다.
- 한글 IME 조합 중 태그 입력이 깨지지 않도록 보완했다.

### 출결 관리 레이아웃 흔들림

- 빠른 상태 입력으로 출결을 저장할 때 기록 전후 UI 높이가 달라져 화면이 움직이던 문제를 수정했다.
- 달력 월 이동 시 5주 달과 6주 달의 높이가 달라지며 레이아웃이 흔들리던 문제를 수정했다.
- 현재 달력은 항상 `42칸(6주)` 기준으로 렌더링해 월마다 카드 높이가 달라지지 않는다.
- 데스크톱에서 상단 `오늘 빠른 입력`/`월 요약` 카드와 하단 `달력`/`빠른 수정 패널`이 같은 2열 폭 기준을 사용하도록 맞춰 시각적인 정렬감을 보정했다.

## 6. 빌드 및 환경 정리

- Windows 환경에서 Node.js와 npm을 설치한 뒤 프론트 빌드를 검증했다.
- 프론트 빌드는 `VITE_BASE_PATH='/app/'` 기준으로 수행한다.
- Windows에서도 안정적으로 빌드되도록 프론트 빌드 스크립트를 정리했다.
- 빌드 후 `processResources`로 최신 정적 자산이 서버에 복사되도록 검증했다.

## 7. 보안 및 데이터 일관성 보강

- 출결 저장은 이제 서비스 레벨에서도 미래 날짜를 막아, 폼 검증을 우회하는 API 경로로도 저장되지 않도록 정리했다.
- `/app` 대시보드, 출결 관리, 훈련장려금 화면은 서버 `monthlySummary` 응답을 단일 소스로 사용하도록 바꿨다.
- 프론트 세션 초기화는 인증 확인 이후 출결/스니펫 로딩이 일부 실패하더라도 로그인 상태 자체를 지우지 않도록 완화했다.
- 클라이언트 IP 해석은 기본적으로 `remoteAddr`를 사용하고, 운영에서만 forwarded header를 신뢰하도록 설정 기반 경계(`APP_SECURITY_TRUST_FORWARDED_HEADERS`)를 추가했다.

## 8. 감사 로그와 개발용 링크 경계 보강

- 출결 감사 로그는 `APP_AUDIT_REQUEST_IP_HMAC_SECRET`가 설정된 경우에만 `request_ip_hmac`를 `HMAC-SHA256`으로 저장하도록 구현했다.
- 출결 API/SSR 저장·수정·삭제 경로가 모두 요청 IP를 서비스까지 전달하도록 정리했다.
- 오래된 `request_ip_hmac`는 기본 30일 보존 뒤 자동으로 `NULL` 처리하는 정리 서비스와 스케줄 잡을 추가했다.
- 복구 이메일 `로컬 확인 링크`는 `app.recovery-email.development-preview-enabled`가 켜진 프로필에서만 생성·노출되도록 제한했다.
- 기본값은 비활성이라 운영 프로필에서는 preview 링크가 내려가지 않는다.
- `local`과 `test` 프로필에서는 개발 편의를 위해 preview 링크를 계속 사용할 수 있다.

## 9. 운영 runbook 추가

- [OPERATIONS_RUNBOOK.md](../operations/OPERATIONS_RUNBOOK.md)를 추가해 `request_ip_hmac` 보존/키 회전 절차와 recovery email SMTP 실메일 점검 절차를 정리했다.
- `request_ip_hmac`는 현재 키 버전 컬럼이 없으므로, 회전 시각과 키 라벨을 앱 외부 운영 기록에 반드시 남겨야 한다는 점을 문서에 명시했다.

## 10. 삭제 요청 / 복구 / 백업 운영 절차 확장

- 운영 runbook에 삭제 요청 등록부 필수 필드, 삭제 취소 절차, purge 활성화 체크리스트를 추가했다.
- recovery email 기반 운영자 보조 계정 복구 절차를 문서화하고, 현재는 전용 maintenance path 또는 실제 rehearsal이 아직 남아 있는 운영 갭임을 명시했다.
- `EC2 + Docker Compose + mysql` 기준의 수동 백업 / 복원 예시와, 복원 후 삭제 계정 scrub 순서를 현재 `AccountDeletionPurgeService` 기준으로 정리했다.
- README, PROJECT_PLAN, SPEC_TRACKER도 현재 운영 문서 상태에 맞게 함께 갱신했다.

## 11. 운영자 보조 비밀번호 초기화 maintenance path 추가

- `OperatorPasswordResetRunner`를 추가해 공개 HTTP 엔드포인트 없이 운영자가 일회성 비밀번호 초기화를 실행할 수 있게 했다.
- 실행 조건은 `검증된 recovery_email 보유`와 `ACTIVE 상태`로 제한했고, 임시 비밀번호도 기존 비밀번호 정책 검증을 통과해야 한다.
- 기본 실행 방식은 `APP_OPERATIONS_PASSWORD_RESET_*` 설정을 일시적으로 주입한 뒤 앱을 한 번 실행하는 형태이며, 성공 후 컨텍스트를 자동 종료한다.
- 기존 세션은 `ActiveMemberSessionFilter`가 현재 저장된 `password_hash`와 세션 principal의 해시를 비교해 다음 보호 요청에서 자동 로그아웃되도록 보강했다.
- 관련 회귀 테스트로 maintenance runner 실행, 운영자 reset 후 기존 세션 차단, 검증된 recovery email / ACTIVE 상태 제약을 확인했다.

## 12. purge one-shot rehearsal runner 추가

- `AccountDeletionPurgeRunner`를 추가해 운영자가 스케줄 활성화 전 purge를 일회성으로 rehearse할 수 있게 했다.
- 실행 조건은 `APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED=true`와 `actor`, `reason` 설정이며, 기본값은 완료 후 컨텍스트 자동 종료다.
- 실행 결과는 `BootSync one-shot account deletion purge completed: purgedCount=...` 로그로 남기고, 스케줄 잡도 처리 건수를 별도 로그로 남기도록 보강했다.
- 관련 회귀 테스트로 due 시각이 지난 `PENDING_DELETE` 계정만 제거되고, 미래 `delete_due_at` 계정은 유지되는지 확인했다.
- README, 운영 runbook, 계획 문서를 현재 경로에 맞게 함께 갱신했다.

## 13. 배포/운영 정합성 보강

- Docker 이미지 빌드가 `frontend`를 함께 production build 하도록 바꿔, `docker compose up --build` 경로에서도 `/app` 프론트가 누락되지 않게 정리했다.
- 복구 이메일 재발송 cooldown은 실제 구현과 사용자 결정에 맞춰 명세도 `1분`으로 동기화했다.
- `AccountDeletionCancelRunner`를 추가해 운영자 보조 삭제 취소도 공개 엔드포인트 없이 일회성 maintenance path로 수행할 수 있게 했다.
- 회원가입 프론트는 아이디를 영문 소문자, 숫자, 언더스코어 규칙으로 맞춰 백엔드 검증과 같은 기준을 사용하도록 정리했다.
- 로그아웃 시 삭제할 세션 쿠키 이름은 더 이상 하드코딩하지 않고 현재 설정값을 따르도록 보강했다.
- 테스트 프로필 H2 datasource는 컨텍스트별 고유 메모리 DB를 사용하도록 바꿔, 전체 스위트에서 다른 `create-drop` 컨텍스트가 공유 DB를 비워 purge 테스트가 흔들리던 문제를 막았다.

## 14. Findings 후속 정리

- `frontend/dist`가 없는 환경에서도 옛 화면으로 돌아가지 않도록, `/app/...` 요청은 프론트 빌드 안내 화면을 보여주게 정리했다.
- `/login`, `/signup`, `/dashboard`, `/attendance`, `/snippets`, `/settings`, 기존 recovery email verify GET 경로는 모두 최신 `/app/...` 경로로 리다이렉트되도록 바꿨다.
- React 프론트의 실제 운영 코드가 `mock-data` 모듈을 직접 가져오지 않도록 공용 타입을 `app-types.ts`, 표시/태그 유틸을 `display.ts`로 분리했다.
- 프론트 패키지 메타데이터도 `starter`에서 `bootsync-frontend`로 정리해 문서, 빌드 로그, 프로젝트 정체성이 서로 맞도록 맞췄다.
- Spring Security의 로그인/로그아웃/비활성 세션/rate limit 리다이렉트도 `/app/login`, `/app/dashboard` 기준으로 통일했다.
- 레거시 GET 화면을 기대하던 통합 테스트는 `리다이렉트 검증 + API 검증` 기준으로 재정렬했다.

## 15. 레거시 프론트 완전 제거

- 옛 SSR 페이지 컨트롤러를 제거하고, GET 호환 라우트만 `LegacyRouteRedirectController`로 통합했다.
- 기존 `attendance`, `auth`, `dashboard`, `settings`, `snippets`, `fragments` 템플릿 파일은 모두 삭제했다.
- React `/app` 프론트가 유일한 사용자 화면이 되었고, 옛 URL은 새 화면으로 보내는 리다이렉트만 남는다.
- 출결/설정/스니펫의 옛 폼 POST 테스트는 API 기준 회귀 테스트로 바꿔, 레거시 템플릿이 없어도 전체 테스트가 통과하도록 정리했다.
- 대시보드 SSR 전용 DTO/서비스와 스니펫 최근 카드 조회 같은 죽은 코드도 함께 제거했다.

## 16. 명세 본문과 프론트 스캐폴드 정리

- `BOOTSYNC_SPEC_V2.md` 본문에서 남아 있던 `Thymeleaf`, `HTMX`, hidden method 폼, 전용 검색 API 기준 설명을 현재 React `/app` + same-origin API 구조에 맞게 갱신했다.
- 프론트 전용 문서 [frontend/README.md](../../frontend/README.md)도 Vite 기본 템플릿 문구 대신 BootSync 실제 구조 설명으로 교체했다.
- 더 이상 사용하지 않는 `src/components/home.tsx`, `src/tempobook`, `tempo.config.json` 같은 프론트 스캐폴드 잔여물 정리 작업을 진행했다.
- Vite 설정의 `optimizeDeps`에서 `tempobook` 경로를 제거해 현재 실제 앱 진입점만 기준으로 최적화하도록 정리했다.
- 프론트 루트 `.gitignore`에서도 `tempobook` 전용 ignore 규칙을 제거했다.

## 17. 검증 이력

- `npm ci`
- `npm run build`
- `.\gradlew.bat processResources`
- `.\gradlew.bat compileJava compileTestJava`
- `.\gradlew.bat test`

작업 중 기능 변경이 클 때는 위 명령을 여러 차례 나눠 반복 실행해 회귀를 확인했다.

## 18. 현재 남은 리스크와 후속 과제

- 옛 프론트 파일은 제거됐지만, `/login`, `/dashboard` 같은 옛 URL은 외부 링크 호환을 위해 리다이렉트 경로로 유지한다.
- `request_ip_hmac` runbook은 정리됐지만, 실제 운영 시 첫 키 회전 기록과 월간 점검 루틴을 남겨야 한다.
- 운영 SMTP 실메일 점검, purge one-shot rehearsal/실제 활성화, 운영 AWS 자격증명 기준 S3 업로드, prod-like 복원 리허설은 아직 실제 운영 증적이 없다.
- 운영자 보조 비밀번호 초기화 maintenance path는 구현됐지만, 실제 운영 티켓 기준 rehearsal 기록이 더 필요하다.

## 19. 참고 문서

- 명세: [BOOTSYNC_SPEC_V2.md](../spec/BOOTSYNC_SPEC_V2.md)
- 전체 계획: [PROJECT_PLAN.md](../planning/PROJECT_PLAN.md)
- 명세 추적: [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md)
- 실행 및 현재 상태: [README.md](../../README.md)
- 운영 점검 절차: [OPERATIONS_RUNBOOK.md](../operations/OPERATIONS_RUNBOOK.md)

## 20. 출결 레이아웃 흔들림 안정화

- `/app/attendance` 데스크톱 화면에서 날짜 선택 시 오른쪽 패널 높이 변화 때문에 하단 2열 grid가 함께 늘어나며 캘린더 카드가 미세하게 흔들려 보이던 문제를 정리했다.
- 하단 2열 레이아웃은 `items-start` 기준으로 바꿔, 오른쪽 패널 높이가 달라져도 왼쪽 캘린더 카드가 같이 stretch되지 않도록 했다.
- 비선택 상태의 빠른 수정 패널도 실제 선택 패널과 비슷한 높이와 섹션 구조를 갖도록 바꿔 첫 클릭 시 레이아웃 점프를 줄였다.
- 출결 날짜 버튼과 패널 액션 버튼의 `transition-all`은 더 좁은 속성 전환으로 바꿔 선택 시 시각적 흔들림을 줄였다.
- 반복 삭제 흐름을 고려해 `기록 삭제` 액션을 패널 하단에서 상단 상태 카드 쪽으로 올렸다.
- 날짜 삭제 후에는 패널을 닫지 않고 같은 날짜를 계속 선택한 상태로 유지해, 삭제 결과를 바로 확인하고 다음 날짜로 자연스럽게 넘어갈 수 있게 했다.
- 저장 성공/실패 안내 메시지 영역도 고정 높이 슬롯으로 바꿔, 메시지가 나타날 때 `빠른 상태 입력` 버튼 묶음이 아래로 밀리지 않도록 정리했다.
- 상태 요약 카드도 `상태/액션 행`과 `안내 문구 행`을 분리한 2행 고정 구조로 바꿔, 기록 유무에 따라 삭제 버튼 폭이 달라져도 `빠른 상태 입력` 섹션 시작점이 변하지 않도록 보강했다.
- 상태 문구와 안내 문구 자체도 각각 고정 높이 슬롯에 넣어, `아직 저장된 기록이 없습니다.`처럼 긴 문구가 두 줄로 바뀌어도 상태 입력 버튼 묶음 위치가 달라지지 않도록 추가 보강했다.
- 이후 사용성 기준으로 다시 다듬어, 상태 문구는 `현재 상태: 미저장/출석/...`처럼 짧은 한 줄 기준으로 통일했다.
- 저장 결과 안내 슬롯은 `빠른 상태 입력` 아래로 옮겨, `선택한 날짜`와 상태 버튼 사이의 불필요한 빈공간을 줄이고 사용자가 먼저 보는 액션 영역을 더 가깝게 배치했다.
- `지난 달 기록을 수정하고 있습니다.` 배너는 정보 대비 공간 점유가 크고 실사용자 문맥에서는 중복 설명에 가까워 제거했다. 지난달 여부는 월 헤더와 캘린더 문맥만으로 충분히 이해할 수 있게 유지했다.
- 2026-03-14: 리뷰에서 확인한 문서-코드 갭 두 건을 정리했다. `AttendancePage`가 주말 입력을 막아 명세의 `명시 저장 기준`과 어긋나던 부분을 수정해 `/app/attendance`에서 미래 날짜만 차단하고 주말도 필요 시 직접 기록할 수 있게 맞췄다. 또 레거시 리다이렉트가 넘겨주던 `yearMonth`, `editId`, `q`, `tag` 문맥을 새 React 화면이 실제로 읽도록 보강했고, `GET /api/attendance/record/{id}` 조회와 MVC 회귀 테스트를 추가해 옛 `/attendance?...` 링크와 `/snippets?...` 링크가 같은 문맥으로 이어지게 정리했다.
- 2026-03-14: 수동 시나리오 테스트에서 나온 UX 이슈도 반영했다. 출결 패널에서 메모를 먼저 입력한 뒤 상태 버튼을 눌렀을 때 메모가 같이 저장된다는 점이 더 분명하게 보이도록 성공 메시지를 바꿨고, 복구 이메일 재발송은 프론트에서도 성공 직후 1분 cooldown 안내를 바로 보여 주며 버튼을 잠시 비활성화하도록 보강했다. 또 새 학습 노트 저장 후 상세가 아니라 목록으로 돌아가게 바꾸고, 사이드바 `학습 노트` 메뉴는 현재 검색/태그 상태를 비운 기본 목록으로 돌아가도록 정리했다.
- 2026-03-14: 후속 수동 테스트에서 나온 두 가지 회귀도 바로 정리했다. `SnippetsPage`는 검색/태그 필터를 `useEffect`로 다시 쓰는 방식 대신 URL 쿼리를 단일 소스로 직접 갱신하도록 바꿔, 필터가 걸린 상태에서도 사이드바 `학습 노트` 초기화와 다른 메뉴 이동이 멈추지 않게 했다. `AttendancePage`는 메모를 적은 뒤 상태 버튼을 눌렀을 때 `메모 저장` 버튼이 곧바로 잠겨 혼란을 주던 조건을 완화해, 상태 저장 후에도 버튼이 남아 있고 중복 저장 시에는 안내 메시지로 처리하도록 맞췄다.
- 2026-03-14: 복구 이메일 재발송 테스트 경로도 보강했다. `SettingsPage`는 pending 인증 대상이 있으면 기존 이메일 인증 여부와 무관하게 재발송 버튼을 보여 주도록 조건을 넓혔고, 복구 이메일 변경 직후의 성공 카드에서도 `로컬 확인 링크` 옆에서 바로 재발송과 1분 cooldown 안내를 확인할 수 있게 했다.

## 21. 프론트 소스 디렉터리 명칭 정리

- 루트의 프론트 소스 디렉터리 이름을 `Front-End`에서 `frontend`로 바꿨다.
- 실무에서 더 일반적인 소문자 단일 이름 기준으로 정리해 경로 입력, 문서, Dockerfile, Gradle 스크립트, 안내 템플릿이 덜 헷갈리도록 맞췄다.
- 빌드 결과물 경로도 `frontend/dist` 기준으로 다시 연결했고, Docker build와 `/app` 정적 자산 복사 흐름이 그대로 유지되는지 함께 검증했다.

## 22. 로컬 운영 rehearsal 3종 기록

- `OpsRehearsalSeedRunner`를 추가해 `local`/`test`에서 maintenance runner 대상 계정과 관련 데이터(seed)를 한 번에 준비할 수 있게 했다.
- [Invoke-LocalOpsRehearsals.ps1](../../scripts/ops/Invoke-LocalOpsRehearsals.ps1)를 추가해 MySQL 기동, seed, 운영자 보조 비밀번호 초기화, purge one-shot, 복원 후 scrub rehearsal을 한 번에 반복 실행할 수 있게 했다.
- 2026-03-15 02:38 KST 기준 로컬 rehearsal을 실제로 수행했고, 결과는 [2026-03-15-ops-rehearsal.md](../reports/ops/2026-03-15-ops-rehearsal.md)에 남겼다.
- password reset rehearsal에서는 `ops_reset_target`의 `updated_at`이 `2026-03-15 02:33:28.533007`에서 `2026-03-15 02:38:35.685551`로 바뀌고 runner 완료 로그가 남는 것을 확인했다.
- purge one-shot rehearsal에서는 `ops_purge_due_target`의 `member`, `attendance_record`, `snippet`, `snippet_tag`, `tag`, `recovery_email_verification_token`가 모두 `1 -> 0`으로 줄고 `attendance_audit_log`는 `NULL/NULL/NULL` 비식별화 상태가 되는 것을 확인했다.
- 복원 후 scrub rehearsal에서는 `ops_scrub_target`에 대해 runbook 순서와 같은 SQL을 적용해 동일하게 모든 본 데이터가 `0`이 되고 audit row가 `NULL/NULL/NULL` 상태가 되는 것을 확인했다.
- 이번 기록으로 로컬 rehearsal 증적은 확보했지만, 운영 SMTP 실메일, purge 스케줄 첫 실행, S3 백업/복원 실측, 정책 문서는 여전히 남아 있다.

## 23. 로컬 백업/복원 자동화 rehearsal

- [Invoke-MySqlBackupToS3.ps1](../../scripts/ops/Invoke-MySqlBackupToS3.ps1)와 [Invoke-MySqlRestoreFromS3.ps1](../../scripts/ops/Invoke-MySqlRestoreFromS3.ps1)를 추가해 `docker mysql -> sql dump -> manifest/report -> restore` 흐름을 PowerShell 스크립트로 고정했다.
- 2026-03-15 03:03 KST에 `-SkipUpload` 기준 백업 rehearsal을 수행해 `build/ops-backup/bootsync-20260315-030309.sql` dump, manifest, markdown report를 생성했다.
- 생성된 dump의 크기는 `13,897 bytes`, SHA-256은 `62CABAFB36A39DE39AB08D2D4C034F89BB3F223F3460296F79EA2EF50DD1CE13`이었다.
- 2026-03-15 03:05 KST에는 별도 `mysql:8.4` 컨테이너 `bootsync-mysql-restore-test`에 같은 dump를 복원했고, `member = 2`, `attendance_record = 5`, `snippet = 3`으로 원본과 같은 row count를 확인했다.
- 상세 기록은 [2026-03-15-backup-restore-rehearsal.md](../reports/ops/2026-03-15-backup-restore-rehearsal.md)에 남겼다.
- 이번 기록은 로컬 기능 검증이며, 운영 AWS 자격증명으로 실제 `daily/weekly` S3 object가 생성되는지와 prod-like 복원 `RTO 8시간`은 아직 별도 증적이 필요하다.

## 24. 대시보드 수강 리스크 기능팩 추가

- 1차 구현으로 `/app/dashboard`에 `수강 리스크` 카드를 추가해, 기존 월 요약만으로도 `지각/조퇴` 누적이 언제 결석 환산으로 넘어가는지와 이번 달 예상 공제 상태를 바로 읽을 수 있게 했다.
- 같은 1차 단계에서 수료 `D-day`와 이번 달 예상 장려금을 함께 보여 주도록 정리해, 서비스 첫 화면에서 “왜 이 서비스를 써야 하는지”가 더 직접적으로 보이게 보강했다.
- 2차 구현으로 백엔드 `trainingSummary` 응답을 추가하고 `app.training` 설정에 과정 시작일/종료일/휴강일 목록을 연결했다.
- `trainingSummary`는 주말/휴강일을 제외한 실제 과정 일정 기준으로 `이번 달 수업일`, `남은 수업일`, `과정 기준 출석률`, `80% 기준 여유`, `미입력 수업일`을 계산한다.
- 이 과정 기준 수치는 대시보드 참고용 안내이며, 기존 `monthlySummary`와 훈련장려금 공식 집계 로직 자체는 바꾸지 않도록 경계를 유지했다.
- 검증은 1차에서 `npm run lint`, `npm run build`, 2차에서 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava test`까지 다시 통과시켰다.

## 25. 사용자별 과정 설정으로 일반화

- 초기 2차 구현은 `app.training` 전역 설정을 기준으로 수강 리스크를 계산했지만, 서비스 타깃을 특정 과정이 아니라 `국비 수강생 전체`로 넓히기 위해 사용자별 `내 과정 정보` 저장 구조로 리팩터링했다.
- `member_training_profile` 테이블과 `V2__member_training_profile.sql` 마이그레이션을 추가해, 당시 기준으로 각 사용자가 과정명, 시작일/종료일, 수료 기준 출석률, 월 기본 장려금, 결석 차감액, 수업 요일, 휴강일을 직접 저장할 수 있게 했다.
- `/api/settings/training-profile` GET/PUT/DELETE API를 추가하고, `/app/settings`에 `내 과정 정보` 편집 화면을 연결했다.
- `monthlySummary`는 이제 사용자별 과정 설정이 있으면 개인 장려금 규칙을 우선 사용하고, 설정이 없으면 기존 기본 규칙으로 계산하도록 유지했다.
- `trainingSummary`는 개인 과정 설정이 있는 사용자에게만 내려 주어, 설정이 없는 사용자는 대시보드에서 월 요약과 장려금은 계속 쓰되 과정 기준 리스크는 설정 안내만 보게 했다.
- purge 서비스와 관련 테스트도 새 설정 테이블을 함께 정리하도록 보강해 계정 삭제 시 사용자별 과정 설정이 남지 않도록 맞췄다.
- 이번 리팩터링 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava test` 전체 통과 기준이다.

## 26. 과정 현황 화면으로 정보 구조 재정리

- 대시보드의 `수강 리스크` 카드는 길게 내려가는 상세 설명을 걷어내고, 수료 `D-day`, `80%` 기준 여유, 이번 달 결석 환산, 지각/조퇴 경고만 짧게 보여 주도록 축소했다.
- 사이드바에 `과정 현황` 메뉴를 추가하고, 과정 기준 출석률/남은 수업일/미입력 수업일/월 공제 상태를 한 번에 보는 전용 상세 페이지를 새로 만들었다.
- `내 과정 정보`는 더 이상 `/app/settings` 안에 두지 않고, `과정 현황` 페이지 안에서 읽기 전용 요약을 먼저 보여 준 뒤 필요할 때만 수정 폼을 열도록 바꿨다.
- 이후 후속 UX 수정으로 `내 과정 정보` 카드 자체가 읽기/편집 모드로 전환되게 바꿔, 수정 버튼을 누른 뒤 아래 별도 섹션으로 스크롤해야 하던 불편을 없앴다.
- 당시 확인 가능했던 과정 기준 규칙을 반영해, 그 시점에는 `결석 차감액`을 직접 입력하지 않고 `월 기본 장려금 ÷ 20일` 공식으로 자동 계산되게 바꿨다. 이 규칙은 이후 27번 항목에서 실제 지급 기록 기준으로 다시 교체됐다.
- `/app/settings`는 기본 프로필, 비밀번호, 복구 이메일, 계정 삭제 같은 진짜 계정 설정만 남기고, 기존 위치를 찾는 사용자를 위해 `과정 현황`으로 안내하는 카드만 유지했다.
- 이번 구조 개편 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava test` 통과 기준이다.

## 27. 실제 지급내역 기준 장려금 모델 전환

- 실제 지급 기록과 과정 기준을 대조해, 기존 `월 기본 장려금 - 결석 차감액` 방식보다 `1일 지급액 × 지급 반영 일수` 모델이 실제 지급 흐름과 맞는다는 점을 확인했다.
- 이에 따라 기본 장려금 규칙을 `1일 지급액 15,800원`, `지급 상한 20일`로 재정의했고, 사용자별 과정 정보도 `월 기본 장려금/결석 차감액` 대신 `1일 지급액/지급 상한 일수`를 저장하도록 바꿨다.
- `member_training_profile`에는 `daily_allowance_amount`, `payable_day_cap` 컬럼을 추가하는 `V3__training_profile_allowance_model.sql` 마이그레이션을 넣고, 기존 저장값은 가능한 범위에서 새 모델로 옮기도록 정리했다.
- `/api/attendance` 응답에는 `allowanceSummary`를 추가해 현재 단위기간의 시작일/종료일, 인정 출석일수, 지급 반영 일수, 예상 장려금을 함께 내려 주도록 보강했다.
- 대시보드와 훈련장려금 페이지는 이제 `현재 단위기간` 기준 금액을 표시하고, 출결 관리의 월간 카드와 과정 리스크는 기존처럼 캘린더 월 기준 집계를 유지한다.
- 실제 지급기간은 개강일 기준 1개월 단위라서, 현재 달 요청은 `오늘` 기준 active period를 사용하고 과거 달은 해당 월말 기준 period를 사용하도록 계산 경계를 정리했다.
- 이번 변경 검증은 `.\gradlew.bat compileJava compileTestJava`, `.\gradlew.bat test`, `npm run lint`, `npm run build` 기준으로 통과했다.

## 28. 출결 관리 빠른 입력 UX와 비수업일 가드 정리

- `/app/attendance` 상단 카드와 오른쪽 패널이 모두 즉시 입력용처럼 보여 역할이 겹치던 문제를 정리했다. 상단 카드는 `오늘 출결` 요약과 `오늘 기록 열기` 진입 버튼만 남기고, 실제 상태/메모 입력은 날짜 선택 패널로 일원화했다.
- 사용자별 `내 과정 정보`에 저장한 수업 요일/휴강일을 출결 달력에도 연결해, 비수업일은 기본적으로 비활성화하고 잘못된 클릭을 줄이도록 보강했다.
- 다만 이미 저장된 기록이 있는 날짜는 비수업일이어도 다시 열어 수정할 수 있게 예외를 두어, 기존 데이터 정리나 과거 오입력 수정 흐름은 막지 않도록 유지했다.
- 이번 변경 검증은 `npm run lint`, `npm run build` 기준으로 통과했다.

## 29. 빈 수업일 일괄 출석 1차 구현

- 출결 입력의 가장 큰 병목이던 과거 기록 백필 문제를 줄이기 위해 `/app/attendance`에 `빈 수업일 일괄 출석` 액션을 추가했다.
- 이 기능은 저장된 `내 과정 정보`를 기준으로 과정 시작일부터 오늘까지의 날짜를 훑어, 비어 있는 수업일만 기본 `출석`으로 생성한다.
- 이미 기록이 있는 날짜, 휴강일, 비수업일, 미래 날짜는 자동으로 제외해 기존 데이터와 충돌하지 않도록 했다.
- 백엔드에는 `POST /api/attendance/bulk-fill/present` 엔드포인트를 추가하고, 생성 건마다 기존 출결 생성과 동일하게 감사 로그를 남기도록 맞췄다.
- 통합 테스트로 `과정 정보 미설정 시 거절`, `설정된 수업일만 생성`, `기존 기록/휴강일 제외`, `감사 로그 생성`을 함께 고정했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat test` 통과 기준이다.

## 30. 출결 상단 밀도와 과정 현황 문구 정리

- `/app/attendance` 상단의 `오늘 출결` 카드가 너무 높아 캘린더가 아래로 밀리던 문제를 줄이기 위해, 오늘 카드 자체를 한 줄 요약형으로 압축했다.
- `빈 수업일 일괄 출석` 액션은 오늘 카드에서 빼고 달력 카드 상단으로 옮겨, 초기 백필 기능은 유지하되 캘린더 진입 위치가 더 위로 오도록 정리했다.
- 기존 `3월 요약` 카드에는 과정 기준 `전체 누적` 출결 4종(출석/지각/조퇴/결석)을 추가해, 오른쪽 카드의 빈 공간을 줄이고 한 화면에서 월간/전체 흐름을 함께 보게 했다.
- 이를 위해 `trainingSummary` 응답에 전체 과정 기준 출석/지각/조퇴/결석 누적 count를 추가했다.
- `/app/course-status`의 `80% 기준 여유`는 사용자가 현재 단위기간 기준으로 오해하지 않도록 `수료 기준 80% 여유`, `전체 과정 기준` 문구로 명확히 바꿨다.
- `/app/settings`에 남아 있던 `과정 정보는 이제...` 안내 카드는 사용자가 굳이 볼 필요가 없는 중간 안내라 제거했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat test` 통과 기준이다.

## 31. 출결 상단 카드 밀도와 패널 문구 추가 다듬기

- `/app/attendance`의 `오늘 출결` 카드는 상태 pill, 날짜, 짧은 안내 문장만 남기는 더 얇은 구조로 다시 줄여, 상단 빈 공간을 줄이고 캘린더 진입이 더 빠르게 보이도록 정리했다.
- 오른쪽 요약 카드는 `전체 누적 -> 이번 달 요약` 순서를 유지한 채 padding, 헤더, 숫자 타일을 더 촘촘하게 다듬어 불필요한 시각적 부피를 줄였다.
- `빠른 수정 패널` 미리보기 카드의 보조 문구를 `바로 저장`으로 줄이고 nowrap 스타일을 고정해, 좁은 카드에서 글자가 어색하게 줄바꿈되던 문제를 막았다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 32. 출결 상단 높이 정렬과 월 이동 흔들림 완화

- `/app/attendance` 상단 2개 카드는 데스크톱에서 같은 높이로 맞추고, `오늘 출결` 카드 내용은 가운데 정렬에 가깝게 재배치해 카드 높이 불균형 때문에 남아 보이던 여백을 줄였다.
- 오른쪽 `전체 누적` 카드는 월 이동 직후에도 안내 박스 대신 동일 높이의 중립 타일을 먼저 보여 주도록 바꿔, 월 전환 시 카드 높이가 잠깐 바뀌며 화면이 움찔하는 느낌을 줄였다.
- 월 이동 버튼과 `오늘 기록 열기` 버튼에는 클릭 직후 blur를 넣고, 검색 파라미터 동기화는 layout effect로 옮겨 데스크톱에서 월 전환 시 한 박자 늦게 다시 그려지는 느낌을 완화했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 33. 오늘 출결 카드를 정보 중심으로 재구성

- `/app/attendance`의 `오늘 출결` 카드는 더 이상 단순 진입 버튼만 두지 않고, 오늘 상태 배지와 메모 미리보기를 카드 안에서 바로 읽을 수 있게 바꿨다.
- 수업일인데 아직 기록이 없으면 `오늘 출결 입력`을 가장 눈에 띄게 보여 주고, 이미 기록이 있으면 `오른쪽 패널에서 수정`으로 바꿔 행동 의미가 더 분명해지도록 정리했다.
- 비수업일은 기본적으로 안내 문구만 보여 주고, 비수업일에 저장된 기록이 남아 있는 예외 상태는 `예외 기록` 맥락을 함께 설명해 오해를 줄였다.
- 오늘 카드의 액션 버튼은 새 화면을 열지 않고 오른쪽 빠른 수정 패널로 포커스를 이동시켜, 한 화면 안에서 출결 확인과 수정이 이어지도록 연결했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 34. 과정 리스크 문구를 일반 사용자 표현으로 단순화

- `수료 기준 80% 여유`처럼 의미를 바로 이해하기 어려운 문구를 `앞으로 더 빠져도 되는 날`로 바꿨다.
- 이 값은 전체 과정 기준으로 수료 최소 출석률을 유지하면서 앞으로 더 비울 수 있는 여유를 뜻한다는 설명을 카드와 리스크 문구에 함께 붙였다.
- 여유가 없는 경우도 `주의 필요` 대신 `여유 없음`, `수료 기준을 맞추기 어려워요`처럼 더 직접적인 표현으로 바꿔 해석 부담을 줄였다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 35. 출결 관리 전체 누적을 HRD식 집계로 전환

- `/app/attendance` 오른쪽 `전체 누적` 카드는 더 이상 raw `출석/지각/조퇴/결석` 건수를 메인으로 보여 주지 않고, `실시일수 / 공식 출석일 / 공식 결석일 / 출석률`을 앞에 두는 HRD식 집계로 바꿨다.
- 공식 출석일은 `입력된 수업일 - (결석 + 지각 3회 환산 + 조퇴 3회 환산)` 기준으로 계산된 `effectivePresentDays`를 사용하고, 공식 결석일은 `effectiveAbsenceCount`를 사용한다.
- 사용자가 실제 입력한 raw 상태 건수는 카드 아래 보조 설명으로 내려 `출석 88, 지각 1, 조퇴 15, 결석 6` 같은 원본 입력 내역도 함께 확인할 수 있게 했다.
- 아직 입력되지 않은 지난 수업일이 있으면 그 날짜가 공식 출석일 계산에서 제외된다는 안내도 함께 보여 주도록 정리했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 36. 출결 월 이동 중 요약 카드 흔들림 완화

- `/app/attendance`에서 다른 달로 이동할 때 오른쪽 `전체 누적` 카드가 짧은 fallback 박스로 잠깐 줄어들던 흐름을 없애기 위해, 로딩 중에도 `실시 / 출석 / 결석 / 출석률`과 같은 4칸 구조를 그대로 유지하도록 바꿨다.
- `전체 누적` 하단 안내 박스와 `이번 달 요약` 영역에도 최소 높이를 주어, 응답 대기 중에 카드 내부 높이가 줄었다가 다시 커지는 현상을 줄였다.
- 숫자 타일은 `tabular-nums`를 적용해 값이 바뀔 때 시각적으로 덜 흔들리도록 정리했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 37. 출결 관리 안전장치와 달력 안내 강화

- `빈 수업일 일괄 출석`은 바로 실행하지 않고, 먼저 `기간 / 반영 일수 / 제외 규칙`을 보여 주는 확인 모달을 띄우도록 바꿨다. 이때 반영 일수는 프론트 추정이 아니라 `/api/attendance/bulk-fill/present/preview`로 실제 생성 예정 건수를 받아 사용한다.
- 달력 위에는 `출석 / 지각 / 조퇴 / 결석` 범례를 추가하고, 조퇴 마커는 색과 모양을 다르게 보여 줘 작은 점만 봐도 구분이 쉬워지도록 정리했다.
- `오늘 출결`은 비수업일이고 저장된 기록이 없을 때 더 낮은 안내형 카드로 축소하고, 대신 `다음 수업일` 정보를 같이 보여 줘 상단 공간 낭비를 줄였다.
- `전체 누적` 카드의 `실시` 표현은 더 쉬운 표현인 `수업일`로 바꾸고, 보조 문구로 `지금까지 실제로 진행된 전체 수업일`임을 설명했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat test` 통과 기준이다.

## 38. 출결 관리를 캘린더 중심 구조로 단순화

- 사용자 피드백에 맞춰 달력 위 `범례`를 제거했고, 날짜 아래 상태 점도 다시 더 작은 크기로 줄였다.
- 상단 `오늘 출결` 카드는 출결 관리 화면에서 역할이 겹친다고 보고 제거했다. 이제 출결 관리는 캘린더와 오른쪽 `전체 누적 / 이번 달 요약 / 빠른 수정 패널`만으로 바로 들어가게 정리했다.
- 오른쪽 요약 카드는 상단 전용 영역이 아니라 수정 패널 위로 옮겨, 달력이 페이지에 더 빨리 보이도록 재배치했다.
- 이번 변경 검증은 `npm run lint`, `npm run build`, `.\gradlew.bat compileJava compileTestJava` 통과 기준이다.

## 39. 전체 비교 검증과 문서 정합화 기록

- 2026-03-16 기준으로 [BOOTSYNC_SPEC_V2.md](../spec/BOOTSYNC_SPEC_V2.md), [PROJECT_PLAN.md](../planning/PROJECT_PLAN.md), [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md), [README.md](../../README.md), [final-checkpoint.md](../reports/checkpoints/final-checkpoint.md)를 다시 읽고 현재 코드/문서 상태를 대조했다.
- 이 환경에서는 `git` 명령과 문서 대 코드 스냅샷 비교를 함께 사용해 검증했다.
- 자동 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat test` 전체 통과로 확인했다.
- 비교 과정에서 [README.md](../../README.md)에 남아 있던 예전 `오늘 출결` 설명과 `실시일수` 표현을 현재 캘린더 중심 출결 관리 구조와 `수업일` 표현으로 맞춰 정리했다.
- 문서 허브와 공유용 문서가 최신 상태를 바로 가리키도록 [docs/README.md](../README.md)와 [2026-03-14-release-note.md](../reports/releases/2026-03-14-release-note.md)에 새 검증 체크포인트 링크를 추가했다.

## 40. 남은 운영 리스크 축소를 위한 정책/증적/배포 초안 추가

- 정책 문서 공백을 줄이기 위해 [PRIVACY_POLICY_DRAFT.md](../policies/PRIVACY_POLICY_DRAFT.md), [TERMS_OF_SERVICE_DRAFT.md](../policies/TERMS_OF_SERVICE_DRAFT.md), [ACCOUNT_DELETION_AND_RECOVERY_POLICY.md](../policies/ACCOUNT_DELETION_AND_RECOVERY_POLICY.md) 초안을 추가했다.
- 실제 운영 증적을 바로 남길 수 있도록 [OPERATIONS_EVIDENCE_TEMPLATES.md](../operations/OPERATIONS_EVIDENCE_TEMPLATES.md)에 SMTP 실메일, purge 첫 실행, S3 업로드, prod-like 복원/RTO 템플릿을 추가했다.
- AWS 배포 리스크를 `문서만 있음` 단계에서 더 나아가게 하기 위해 [k8s](../../k8s) 폴더에 namespace, configmap, secret example, deployment, service, ingress 초안 매니페스트를 추가했다.
- [PROJECT_PLAN.md](../planning/PROJECT_PLAN.md), [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md), [README.md](../../README.md), [docs/README.md](../README.md)에 현재 상태를 반영해 정책 문서는 초안 준비 완료, AWS 배포는 문서/매니페스트 준비 후 실제 실행 대기 상태로 정리했다.

## 41. 모니터링 접근 제어와 문서 기준을 현재 상태로 재정렬

- `/actuator/prometheus`가 익명 공개돼 있던 상태는 명세의 내부 접근 원칙과 맞지 않아, 이제는 Bearer 토큰이 있을 때만 응답하도록 앱 보안 필터를 추가했다.
- `application-local.yml`, `application-test.yml`, `application-prod.yml`에 Prometheus scrape 토큰 구성을 명시했고, 테스트도 `무토큰 401 / 올바른 토큰 200` 기준으로 바꿨다.
- `k8s/k8s-bootsync/20-secret.example.yaml`에는 앱용 `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN`을, `k8s/k8s-monitoring/prometheus-scrape-secret.example.yaml`에는 Prometheus 쪽 동일 토큰 템플릿을 추가했다.
- `k8s/k8s-monitoring/prometheus-config.yaml`과 `prometheus-depl_svc.yaml`은 이 토큰을 `Authorization: Bearer ...`로 붙여 BootSync 앱을 스크랩하도록 갱신했다.
- [2026-03-16-validation-checkpoint.md](../reports/checkpoints/2026-03-16-validation-checkpoint.md), [README.md](../../README.md), [k8s/README.md](../../k8s/README.md), [AWS_DEPLOYMENT_CHECKLIST.md](../planning/AWS_DEPLOYMENT_CHECKLIST.md), [AWS_FINAL_PROJECT_GUIDE.md](../planning/AWS_FINAL_PROJECT_GUIDE.md), [PROD_ENV_CHECKLIST.md](../operations/PROD_ENV_CHECKLIST.md), [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md)을 현재 보호 방식과 폴더 구조 기준으로 다시 맞췄다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat test`, `.\gradlew.bat compileJava compileTestJava` 통과를 기준으로 한다.

## 42. Kubernetes health probe 경로를 실제 헬스 응답으로 복구

- `k8s/k8s-bootsync/30-deployment.yaml`은 `readinessProbe=/actuator/health/readiness`, `livenessProbe=/actuator/health/liveness`를 사용하고 있었지만, 앱 보안 설정은 `/actuator/health`만 공개해 실제 컨테이너에서는 두 경로가 로그인 리다이렉트(`302`)로 빠지고 있었다.
- 이 상태는 kubelet 입장에서는 3xx도 성공으로 간주돼 프로브가 실제 상태를 보지 못하는 문제라, `SecurityConfig`와 `ActiveMemberSessionFilter`에서 `/actuator/health/**`를 공개 경로로 정리했다.
- `management.endpoint.health.probes.enabled`는 공통 설정으로 올려 prod뿐 아니라 테스트에서도 같은 프로브 경로를 검증할 수 있게 맞췄다.
- 회귀 테스트로 `WebRoutingTest`에 readiness/liveness가 익명 `200`을 반환하는지 확인하는 항목을 추가했다.
- 최신 비교 검증 문서의 기준 시각도 다시 갱신해, 현재 스냅샷을 가리키는 문서 설명과 실제 내용이 어긋나지 않게 맞췄다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat compileJava compileTestJava`, `.\gradlew.bat test` 통과를 기준으로 한다.

## 43. 출결 관리 전체 누적 출석률 분모를 현재 진행 기준으로 정리

- `/app/attendance`의 `전체 누적` 카드에서 `수업일 / 출석 / 결석`은 이미 지난 수업일 기준으로 보여 주면서, `출석률`만 전체 과정 총수업일을 분모로 다시 나눠 계산하고 있었다.
- 그 결과 모든 지난 수업일을 `출석`으로 채워도, 남아 있는 미래 수업일이 분모에 포함돼 `89.5%`처럼 낮게 보이는 화면 불일치가 발생했다.
- 화면은 이제 프론트에서 별도 재계산하지 않고, 백엔드 `TrainingSummaryService`가 내려주는 `attendanceRatePercent`를 그대로 사용해 현재까지 기준 출석률과 일치하게 맞췄다.
- 출석률이 정확히 `100`일 때는 `100.0%` 대신 `100%`로 보이도록 표기 함수를 한 번 더 다듬었다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build` 통과를 기준으로 한다.

## 44. README 역할 구분을 명확하게 정리

- 루트 [README.md](../../README.md)에 `README 안내` 표를 추가해, 처음 보는 사람도 `프로젝트 전체 안내 / 문서 허브 / Kubernetes 안내` 중 어디를 봐야 하는지 바로 알 수 있게 정리했다.
- [docs/README.md](../README.md)에는 `어떤 README를 먼저 볼까` 표를 추가해, 세부 문서 허브와 루트 README, `k8s/README.md`의 역할 차이를 분명히 적었다.
- [k8s/README.md](../../k8s/README.md) 상단에도 이 문서가 `Kubernetes 전용 안내`라는 설명을 넣어, 로컬 실행 안내와 섞어 읽지 않도록 정리했다.
- 이번 변경은 문서 구조 안내만 다룬 작업이라 별도 빌드/테스트는 실행하지 않았다.

## 45. 과정 현황 출석률 표현과 운영 범위 문구를 현재 구조에 맞춤

- `과정 현황` 화면의 출석률 값은 이미 `현재까지 진행된 수업일` 기준으로 계산되고 있었지만, 화면 라벨이 `전체 과정 출석률`처럼 읽혀 혼동을 줄 수 있어 `현재까지 출석률`로 바꾸고 보조 문구에도 계산 기준을 명시했다.
- 루트 [README.md](../../README.md)와 [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md)도 같은 의미로 표현을 맞춰, 현재 화면과 문서 설명이 어긋나지 않게 정리했다.
- [BOOTSYNC_SPEC_V2.md](../spec/BOOTSYNC_SPEC_V2.md)의 `v1 제외 기능`에는 Kubernetes를 `제품 기능의 필수 출시 범위는 아님`으로 풀어 쓰고, 대신 `AWS 배포` 준비 과정에서 운영용 매니페스트와 모니터링 템플릿을 저장소에 둘 수 있다는 설명을 추가했다.
- `member_training_profile`의 구 장려금 컬럼을 제거하는 [V4__drop_legacy_training_profile_allowance_columns.sql](../../src/main/resources/db/migration/V4__drop_legacy_training_profile_allowance_columns.sql)에 정리 목적 주석을 추가해, 새 모델로 전환된 뒤 남은 스키마 정리 단계임을 바로 알 수 있게 했다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat compileJava compileTestJava` 통과를 기준으로 한다.

## 46. 출결 관리 상단 요약 바와 빠른 입력 동선을 재배치

- `전체 누적`과 `이번 달 요약`을 오른쪽 세로 카드에서 분리해, 화면 상단의 얇은 가로 요약 바로 옮겼다.
- 상단 카드 안에서는 두 요약을 좌우 2구역으로 나눠 한 번에 훑을 수 있게 하고, 긴 안내 박스는 줄여 카드 높이가 과도하게 커지지 않도록 정리했다.
- 오른쪽 영역은 요약 카드 대신 `빠른 수정 패널`만 남겨, 데스크톱에서 `요약 확인 -> 날짜 선택 -> 바로 상태 입력` 흐름이 더 짧아지도록 맞췄다.
- 루트 [README.md](../../README.md)의 출결 관리 설명도 새 레이아웃에 맞게 상단 요약 바 중심으로 갱신했다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat compileJava compileTestJava` 통과를 기준으로 한다.

## 47. 비선택 상태 빠른 수정 패널을 안내형으로 축소

- 출결 관리 화면에 처음 들어왔을 때는 날짜를 아직 고르지 않았는데도 `빠른 상태 입력`과 `메모`가 비활성화된 채 길게 보여, 첫 화면 밀도가 떨어지고 불필요한 빈 공간이 커 보였다.
- 비선택 상태 패널은 이제 `빠른 수정 패널` 설명과 `선택 전 미리보기`만 남기고, 실제 `빠른 상태 입력`과 `메모 저장` 영역은 날짜를 선택했을 때만 열리도록 바꿨다.
- 데스크톱에서는 비선택 상태 패널 최소 높이도 함께 낮춰, 처음 진입했을 때 오른쪽 패널이 과하게 길어 보이지 않도록 정리했다.
- 루트 [README.md](../../README.md)에도 날짜 선택 전에는 설명만 보인다는 현재 동작을 반영했다.
- 이번 변경 검증은 `frontend` 기준 `npm run lint`, `npm run build`, 루트 기준 `.\gradlew.bat compileJava compileTestJava` 통과를 기준으로 한다.
