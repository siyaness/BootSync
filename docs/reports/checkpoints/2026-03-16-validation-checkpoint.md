# BootSync Validation Checkpoint

기준 시각: 2026-03-16 14:33:25 +09:00

이 문서는 2026-03-16 기준 현재 워크스페이스 스냅샷을 다시 검증하고, 기준 문서와 실제 코드/구조가 어디까지 맞는지 정리하기 위한 비교 검증 보고서다.

## 1. 검증 방법

- 기준 문서: [BOOTSYNC_SPEC_V2.md](../../spec/BOOTSYNC_SPEC_V2.md), [PROJECT_PLAN.md](../../planning/PROJECT_PLAN.md), [SPEC_TRACKER.md](../../spec/SPEC_TRACKER.md)
- 사용자/운영 문서: [README.md](../../../README.md), [docs/README.md](../../README.md), [k8s/README.md](../../../k8s/README.md), [PROD_ENV_CHECKLIST.md](../../operations/PROD_ENV_CHECKLIST.md)
- 이전 점검 기준: [final-checkpoint.md](final-checkpoint.md), [WORK_LOG.md](../../history/WORK_LOG.md)
- 코드 비교 기준: 현재 `frontend`, 출결/과정 현황/설정 화면, 보안 설정, `k8s` 매니페스트

참고:

- 현재 환경에서는 `git` 명령 사용이 가능했고, 문서 대 코드 스냅샷 비교와 필요한 파일 diff를 함께 사용했다.
- 자동 검증은 프론트 lint/build와 백엔드 test/compile 태스크를 다시 실행해 확인했다.

## 2. 현재 상태

### 출결 관리

- `/app/attendance`는 상단 `오늘 출결` 카드를 두지 않고, `캘린더 -> 전체 누적/이번 달 요약 -> 빠른 수정 패널` 중심 구조로 정리돼 있다.
- `전체 누적`은 raw 입력 건수가 아니라 HRD식 `수업일 / 공식 출석일 / 공식 결석일 / 출석률`을 앞에 보여 준다.
- `빈 수업일 일괄 출석`은 preview API 기준 `기간 / 생성 예정 일수 / 제외 규칙`을 먼저 보여 준 뒤 확인을 받는다.
- 저장된 `내 과정 정보`가 있으면 비수업일과 휴강일은 달력에서 기본 비활성화되고, 이미 저장된 기록이 있는 날짜만 예외적으로 다시 수정할 수 있다.

### 대시보드/과정 현황

- 대시보드에는 명세와 맞게 `오늘 출결 카드`가 그대로 존재한다.
- `수료 기준 80% 여유` 같은 내부 표현은 `앞으로 더 빠져도 되는 날`처럼 일반 사용자가 바로 이해할 수 있는 문구로 바뀌었다.
- `내 과정 정보`는 `과정 현황` 카드 안에서 읽기/편집 모드를 전환하며, `1일 지급액`과 `지급 상한 일수` 중심의 장려금 규칙을 관리한다.

### 로그인/데모 실행

- `local`/`test` 기본 데모 계정은 `d / d`다.
- 이 값은 데모 시드 계정에만 적용되며, 회원가입/비밀번호 변경의 일반 길이 규칙을 완화한 것은 아니다.

### 운영/모니터링

- `k8s` 폴더는 현재 `k8s-bootsync`, `k8s-monitoring`, `k8s-argocd`로 분리돼 있다.
- `k8s-bootsync`에는 `41-actuator-service.yaml`, `55-certificate.yaml`, `60-hpa.yaml`, `70-poddisruptionbudget.yaml`까지 포함된다.
- `/actuator/health/readiness`와 `/actuator/health/liveness`는 이제 Kubernetes 프로브가 실제 상태를 볼 수 있도록 로그인 리다이렉트 없이 공개된다.
- `/actuator/prometheus`는 더 이상 익명 공개가 아니고, Bearer 토큰이 있어야 응답한다.
- `k8s-monitoring`에는 BootSync scrape 토큰용 `prometheus-scrape-secret.example.yaml`이 추가돼, 앱 Secret과 같은 값을 Prometheus가 읽어 스크랩하도록 맞췄다.

## 3. 자동 검증 결과

실행 명령:

- `frontend`: `npm run lint`
- `frontend`: `npm run build`
- 루트: `.\gradlew.bat test`
- 루트: `.\gradlew.bat compileJava compileTestJava`

결과:

- 모두 통과
- 프론트 타입 검사와 번들 빌드 정상
- 백엔드 전체 테스트 정상
- 백엔드 compile 태스크 정상

## 4. 현재 문서와 코드의 정합성 판단

- [BOOTSYNC_SPEC_V2.md](../../spec/BOOTSYNC_SPEC_V2.md)의 장려금 규칙(`1일 지급액 × 지급 반영 일수`, 지각/조퇴 3회 환산)은 현재 서비스 계산 방식과 맞는다.
- [BOOTSYNC_SPEC_V2.md](../../spec/BOOTSYNC_SPEC_V2.md)의 `GET /actuator/health` 외 Actuator 내부 제한 원칙에 맞춰 `/actuator/prometheus`는 토큰 보호로 정리했다.
- [PROJECT_PLAN.md](../../planning/PROJECT_PLAN.md)의 우선순위는 여전히 운영 준비(`SMTP`, `AWS 배포`, `S3`, `복원`, 정책 문서`)가 맞다.
- [SPEC_TRACKER.md](../../spec/SPEC_TRACKER.md)의 운영/모니터링 상태는 현재 구현과 맞게 Bearer 토큰 보호까지 반영했다.
- 사용자/운영 문서 쪽에서는 [README.md](../../../README.md), [k8s/README.md](../../../k8s/README.md), [AWS_DEPLOYMENT_CHECKLIST.md](../../planning/AWS_DEPLOYMENT_CHECKLIST.md), [AWS_FINAL_PROJECT_GUIDE.md](../../planning/AWS_FINAL_PROJECT_GUIDE.md), [PROD_ENV_CHECKLIST.md](../../operations/PROD_ENV_CHECKLIST.md), [WORK_LOG.md](../../history/WORK_LOG.md)의 설명을 현재 구조 기준으로 다시 맞췄다.

## 5. 현재 기준 남은 리스크

- 운영 SMTP 실메일 스모크 테스트는 아직 실제 증적이 필요하다.
- purge 스케줄 운영 첫 실행 기록은 아직 없다.
- AWS 실제 배포(`ECR -> RDS -> EC2/k3s`)는 아직 시작 전이다.
- Prometheus/Grafana는 템플릿과 앱 보호 로직까지는 준비됐지만 실제 클러스터 설치/대시보드/알람 기준은 아직 운영 증적이 없다.
- 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치는 아직 운영 증적이 없다.
- prod-like 복원 리허설과 `RTO 8시간` 실측은 아직 운영 증적이 없다.
- 개인정보 처리방침, 이용약관, 삭제/복구 정책 문서는 아직 별도 완성본과 공개 운영 값 반영이 필요하다.

## 6. 결론

2026-03-16 기준 현재 코드 스냅샷은 프론트/백엔드 자동 검증을 다시 통과했고, 최근 많이 바뀐 출결 관리, 과정 현황, Prometheus 모니터링 접근 제어, Kubernetes health probe 공개 경로까지 현재 문서 기준으로 다시 정리됐다.

지금 단계의 핵심 미해결 사항은 기능 안정성보다 운영 증적과 실제 AWS 배포 준비다.
