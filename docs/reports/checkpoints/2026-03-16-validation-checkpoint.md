# BootSync Validation Checkpoint

기준 시각: 2026-03-16 00:10:00 +09:00

이 문서는 2026-03-16 기준 현재 워크스페이스 스냅샷을 다시 검증하고, 기존 체크포인트와 기준 문서 대비 무엇이 바뀌었는지 기록하기 위한 비교 검증 보고서다.

## 1. 검증 방법

- 기준 문서: [BOOTSYNC_SPEC_V2.md](/C:/B_Recheck/docs/spec/BOOTSYNC_SPEC_V2.md), [PROJECT_PLAN.md](/C:/B_Recheck/docs/planning/PROJECT_PLAN.md), [SPEC_TRACKER.md](/C:/B_Recheck/docs/spec/SPEC_TRACKER.md)
- 사용자 문서: [README.md](/C:/B_Recheck/README.md), [docs/README.md](/C:/B_Recheck/docs/README.md)
- 이전 점검 기준: [final-checkpoint.md](/C:/B_Recheck/docs/reports/checkpoints/final-checkpoint.md), [WORK_LOG.md](/C:/B_Recheck/docs/history/WORK_LOG.md)
- 코드 비교 기준: 현재 `frontend`, 출결/과정 현황/장려금 관련 서비스와 설정 코드

참고:

- 이 환경에는 `git` 명령이 없어 diff 기반 비교는 수행하지 못했다.
- 대신 기존 체크포인트, 작업 로그, 명세, 실제 코드 상태를 직접 대조하는 방식으로 현재 스냅샷을 검증했다.

## 2. 이전 체크포인트 이후 확인된 현재 상태

### 출결 관리

- `/app/attendance`는 더 이상 상단 `오늘 출결` 카드를 두지 않고, `캘린더 -> 전체 누적/이번 달 요약 -> 빠른 수정 패널` 중심 구조로 정리돼 있다.
- `전체 누적`은 raw 입력 건수가 아니라 HRD식 `수업일 / 공식 출석일 / 공식 결석일 / 출석률`을 앞에 보여 준다.
- 카드 하단에는 `입력한 기록: 출석 / 지각 / 조퇴 / 결석` 원본 건수를 따로 노출해 계산 근거를 확인할 수 있다.
- `빈 수업일 일괄 출석`은 즉시 실행되지 않고, preview API 기준 `기간 / 생성 예정 일수 / 제외 규칙`을 보여 준 뒤 확인을 받는다.
- 저장된 `내 과정 정보`가 있으면 비수업일과 휴강일은 달력에서 기본 비활성화되고, 이미 저장된 기록이 있는 날짜만 예외적으로 다시 수정할 수 있다.

### 과정 현황

- `수료 기준 80% 여유` 같은 내부 표현은 `앞으로 더 빠져도 되는 날`처럼 일반 사용자가 바로 이해할 수 있는 문구로 바뀌었다.
- 이 값은 `현재 단위기간`이 아니라 `전체 과정 기준` 여유임을 설명 문구에 함께 명시한다.
- `내 과정 정보`는 카드 안에서 읽기/편집 모드를 전환하며, `1일 지급액`과 `지급 상한 일수` 중심의 장려금 규칙을 관리한다.

### 로그인/데모 실행

- `local`/`test` 기본 데모 계정은 `d / d`로 바뀌었다.
- 이 값은 데모 시드 계정에만 적용되며, 회원가입/비밀번호 변경의 일반 길이 규칙을 완화한 것은 아니다.

### 문서 정합성

- [README.md](/C:/B_Recheck/README.md)에 남아 있던 예전 `오늘 출결` 설명과 `실시일수` 표현은 현재 UI에 맞게 정리했다.
- 문서 허브는 최신 비교 검증 문서인 이 파일을 우선 진입점으로 가리키도록 업데이트했다.

## 3. 자동 검증 결과

실행 명령:

- `frontend`: `npm run lint`
- `frontend`: `npm run build`
- 루트: `.\gradlew.bat test`

결과:

- 모두 통과
- 프론트 타입 검사와 번들 빌드 정상
- 백엔드 전체 테스트 정상

## 4. 현재 문서와 코드의 정합성 판단

- [BOOTSYNC_SPEC_V2.md](/C:/B_Recheck/docs/spec/BOOTSYNC_SPEC_V2.md)의 장려금 규칙(`1일 지급액 × 지급 반영 일수`, 지각/조퇴 3회 환산)은 현재 서비스 계산 방식과 맞는다.
- [PROJECT_PLAN.md](/C:/B_Recheck/docs/planning/PROJECT_PLAN.md)의 우선순위는 여전히 운영 준비(`SMTP`, `AWS 배포`, `S3`, `복원`, 정책 문서`)가 맞다.
- [SPEC_TRACKER.md](/C:/B_Recheck/docs/spec/SPEC_TRACKER.md)의 `Attendance`, `Course Status`, `Recovery Email` 상태는 현재 구현과 큰 충돌이 없었다.
- 사용자 문서 쪽에서는 [README.md](/C:/B_Recheck/README.md)의 출결 설명만 최신 UI 기준으로 수정이 필요했고, 이번에 반영했다.

## 5. 현재 기준 남은 리스크

- 운영 SMTP 실메일 smoke test는 아직 실제 증적이 필요하다.
- purge 스케줄 운영 첫 실행 기록은 아직 없다.
- AWS 실제 배포(`ECR -> RDS -> EC2/k3s`)는 아직 시작 전이다.
- S3 실제 업로드와 prod-like 복원, `RTO 8시간` 실측은 아직 운영 증적이 없다.
- 개인정보 처리방침, 이용약관, 삭제/복구 절차 문서는 아직 별도 완성본이 필요하다.

## 6. 결론

2026-03-16 기준 현재 코드 스냅샷은 프론트/백엔드 자동 검증을 다시 통과했고, 최근 많이 바뀐 출결 관리와 과정 현황의 실제 동작도 현재 문서에 맞춰 다시 정리됐다.

지금 단계의 핵심 미해결 사항은 기능 안정성보다 운영 증적과 실제 AWS 배포 준비다.
