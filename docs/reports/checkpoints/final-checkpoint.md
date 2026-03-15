# BootSync Final Checkpoint

기준 시각: 2026-03-15 04:24:07 +09:00

이 문서는 최근 프론트 전환, UX 수정, recovery email 흐름 정리, 운영 rehearsal 문서 반영, 문서-코드 정합화 작업까지 포함한 현재 상태의 최종 점검 요약이다.

## 1. 현재 구조 요약

- 사용자 화면은 React `/app` 프론트가 단일 진입점이다.
- 프론트 소스 디렉터리 이름은 `frontend`로 정리돼 있다.
- 옛 SSR 화면 파일은 제거됐고, 옛 URL은 호환용 리다이렉트만 남아 있다.
- 백엔드는 Spring Boot API 중심 구조이며, 출결/학습 노트/설정/복구 이메일 흐름이 same-origin API로 연결된다.
- 문서는 `docs/spec`, `docs/planning`, `docs/operations`, `docs/history`, `docs/reports` 기준으로 역할별 분리 관리한다.

## 2. 이번 점검 범위

- 출결 화면 레이아웃 흔들림과 메모 저장 UX
- 학습 노트 저장 후 이동 흐름
- 학습 노트 검색/태그 필터와 사이드바 이동
- recovery email 재발송 노출 조건과 resend cooldown
- 문서와 실제 코드/테스트 정합성

## 3. 확인된 상태

### 프론트

- `/app/attendance`
  - 달력 42칸 고정으로 월 이동 시 높이 흔들림을 줄였다.
  - 오른쪽 빠른 수정 패널은 상태/삭제/메모 흐름을 데스크톱 기준으로 다시 정리했다.
  - 메모를 입력한 뒤 상태를 바꿔도 저장 흐름이 끊기지 않고, `메모 저장` 버튼이 즉시 막혀 혼란을 주지 않도록 완화했다.
  - 주말도 명세에 맞춰 명시적으로 저장 가능하다.
- `/app/snippets`
  - 새 학습 노트 저장 후 상세가 아니라 목록으로 돌아간다.
  - 검색/태그 필터는 URL 쿼리를 단일 소스로 사용한다.
  - 필터가 걸린 상태에서도 사이드바 `학습 노트` 초기화와 다른 메뉴 이동이 멈추지 않는다.
- `/app/settings`
  - pending recovery email target이 있으면 재발송 버튼이 보인다.
  - 복구 이메일 변경 직후 성공 카드에서도 `로컬 확인 링크`와 `인증 메일 재발송`을 함께 확인할 수 있다.
  - recovery email resend cooldown은 프론트/백엔드 모두 `1분`으로 맞춰졌다.

### 백엔드

- recovery email resend는 `/api/settings/recovery-email/resend` 단일 엔드포인트로 유지한다.
- resend 제한은 계정당 5회/1시간, IP당 10회/1시간, 같은 계정 기준 cooldown 1분이다.
- 미래 날짜 출결 차단, 출결 월 요약 단일 소스, recovery email pending/confirm 흐름, 계정 상태 재검사는 계속 유지된다.
- 단독 Docker 배포 이미지는 기본 `prod` 프로필과 non-root 사용자 `bootsync`를 사용하도록 정리돼 있다.

### 문서

- 기준 문서, 계획 문서, 작업 기록, README 허브가 현재 cooldown 1분 기준과 새 문서 구조에 맞게 동기화돼 있다.
- 로컬 ops rehearsal, backup/restore rehearsal, AWS 최종 프로젝트 가이드도 현재 문서 구조 안에서 바로 추적 가능하다.
- 문서 허브는 `docs/README.md`를 기준으로 바로 진입할 수 있다.

## 4. 검증 결과

### 자동 검증

- `npm run build` 통과
- `.\gradlew.bat test --rerun-tasks` 통과
- `.\gradlew.bat processResources --rerun-tasks` 통과

### 수동 확인 반영

- 로그인/회원가입/대시보드 기본 흐름 정상
- 출결 등록/수정/삭제 정상
- 주말 출결 입력 가능
- 학습 노트 생성/수정/삭제 정상
- recovery email 재발송 노출과 cooldown 정상
- 옛 URL은 최신 `/app` 경로로 연결

## 5. 현재 기준 남은 리스크

- 운영 SMTP 실메일 smoke test는 코드가 아니라 실제 운영 증적이 남아야 한다.
- 로컬 purge one-shot rehearsal과 로컬 backup/restore rehearsal은 완료됐고, 남은 것은 purge 스케줄 첫 실행 기록과 운영 AWS 자격증명 기준 S3 업로드, prod-like 복원/RTO 증적이다.
- AWS 실제 배포(`ECR -> RDS -> EC2/k3s`)와 정책 문서 확정은 아직 남아 있다.
- 브라우저별 세부 UI 확인은 추가로 누적될 수 있으므로, 데스크톱 우선 수동 점검은 릴리즈 직전 한 번 더 권장한다.

## 6. 결론

현재 BootSync는 React `/app` 프론트 단일 구조, 백엔드 API 연결, recovery email resend 1분 cooldown, 출결/학습 노트 주요 UX 수정까지 포함해 로컬 및 비공개 테스트 기준으로 안정적인 상태다.

운영 직전 단계에서 남은 핵심은 코드보다는 운영 증적과 릴리즈 직전 수동 QA 정리다.
