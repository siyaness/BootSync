# BootSync Docs

상세 문서는 이 폴더에서 관리합니다. 앞으로 문서 관련 작업을 시작할 때는 이 파일을 기준으로 필요한 문서를 열면 됩니다.

## 어떤 README를 먼저 볼까

README가 여러 개 보여도 역할은 분리돼 있습니다.

| 파일 | 이런 경우에 먼저 확인 |
|---|---|
| [README.md](../README.md) | 프로젝트를 처음 열었거나, 실행 방법과 현재 화면 동작을 알고 싶을 때 |
| [docs/README.md](README.md) | 명세, 계획, 운영, 작업 기록 문서 위치를 찾고 싶을 때 |
| [k8s/README.md](../k8s/README.md) | Kubernetes 배포 순서와 매니페스트 역할을 확인할 때 |

## 문서 구조

### 기준 문서

- [BOOTSYNC_SPEC_V2.md](spec/BOOTSYNC_SPEC_V2.md): 최종 기준 명세
- [SPEC_TRACKER.md](spec/SPEC_TRACKER.md): 명세 항목별 구현 상태 추적표

### 계획 문서

- [PROJECT_PLAN.md](planning/PROJECT_PLAN.md): 전체 개발 로드맵과 다음 우선순위
- [AWS_FINAL_PROJECT_GUIDE.md](planning/AWS_FINAL_PROJECT_GUIDE.md): 국비 과정 최종 프로젝트 기준의 AWS 배포/운영/발표 가이드
- [AWS_DEPLOYMENT_CHECKLIST.md](planning/AWS_DEPLOYMENT_CHECKLIST.md): 실제 AWS 배포를 시작할 때 따라가는 실행 체크리스트

### 운영 문서

- [OPERATIONS_RUNBOOK.md](operations/OPERATIONS_RUNBOOK.md): 운영 점검, 복구, purge, 백업 절차
- [PROD_ENV_CHECKLIST.md](operations/PROD_ENV_CHECKLIST.md): 운영 배포에 필요한 환경변수와 maintenance 값 체크리스트
- [OPERATIONS_EVIDENCE_TEMPLATES.md](operations/OPERATIONS_EVIDENCE_TEMPLATES.md): SMTP, purge, S3, 복원 실측 기록 템플릿

### 현재 운영 준비 상태

- 로컬 rehearsal과 자동 검증 기록은 이미 존재한다.
- 현재 남은 핵심 운영 과제는 아래 여섯 가지다.
- 운영 SMTP 실메일 스모크 테스트 수행
- purge 스케줄 운영 첫 실행 기록 확보
- AWS 실제 배포 (`ECR -> RDS -> EC2/k3s`)
- 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치
- prod-like 복원 리허설 수행과 `RTO 8시간` 실측
- 정책 문서 확정과 공개 운영 값 반영

### 정책 문서

- [PRIVACY_POLICY_DRAFT.md](policies/PRIVACY_POLICY_DRAFT.md): 개인정보 처리방침 초안
- [TERMS_OF_SERVICE_DRAFT.md](policies/TERMS_OF_SERVICE_DRAFT.md): 이용약관 초안
- [ACCOUNT_DELETION_AND_RECOVERY_POLICY.md](policies/ACCOUNT_DELETION_AND_RECOVERY_POLICY.md): 계정 삭제/복구 절차 공개 안내문

### 이력 및 보고서

- [WORK_LOG.md](history/WORK_LOG.md): 작업 기록과 검증 이력
- [2026-03-15-ops-rehearsal.md](reports/ops/2026-03-15-ops-rehearsal.md): 로컬 운영 rehearsal 3종 실행 기록
- [2026-03-15-backup-restore-rehearsal.md](reports/ops/2026-03-15-backup-restore-rehearsal.md): 로컬 백업/복원 자동화 rehearsal 기록
- [2026-03-14-release-note.md](reports/releases/2026-03-14-release-note.md): 최근 수정 사항을 사람 읽기 좋게 정리한 공유용 문서
- [2026-03-16-validation-checkpoint.md](reports/checkpoints/2026-03-16-validation-checkpoint.md): 현재 스냅샷 기준 전체 비교 검증과 자동 테스트 결과
- [final-checkpoint.md](reports/checkpoints/final-checkpoint.md): 2026-03-15 기준 최종 점검 요약

## 권장 확인 순서

1. 명세 기준 확인: [BOOTSYNC_SPEC_V2.md](spec/BOOTSYNC_SPEC_V2.md)
2. 현재 단계 확인: [PROJECT_PLAN.md](planning/PROJECT_PLAN.md)
3. 최종 프로젝트 방향 확인: [AWS_FINAL_PROJECT_GUIDE.md](planning/AWS_FINAL_PROJECT_GUIDE.md)
4. 실제 배포 순서 확인: [AWS_DEPLOYMENT_CHECKLIST.md](planning/AWS_DEPLOYMENT_CHECKLIST.md)
5. 운영 변수 확인: [PROD_ENV_CHECKLIST.md](operations/PROD_ENV_CHECKLIST.md)
6. 운영 증적 템플릿 확인: [OPERATIONS_EVIDENCE_TEMPLATES.md](operations/OPERATIONS_EVIDENCE_TEMPLATES.md)
7. 정책 초안 확인: [PRIVACY_POLICY_DRAFT.md](policies/PRIVACY_POLICY_DRAFT.md)
8. 구현 상태 확인: [SPEC_TRACKER.md](spec/SPEC_TRACKER.md)
9. 운영 절차 확인: [OPERATIONS_RUNBOOK.md](operations/OPERATIONS_RUNBOOK.md)
10. 최근 작업 이력 확인: [WORK_LOG.md](history/WORK_LOG.md)
11. 최근 운영 rehearsal 확인: [2026-03-15-ops-rehearsal.md](reports/ops/2026-03-15-ops-rehearsal.md)
12. 최근 백업/복원 rehearsal 확인: [2026-03-15-backup-restore-rehearsal.md](reports/ops/2026-03-15-backup-restore-rehearsal.md)
13. 공유용 변경 요약 확인: [2026-03-14-release-note.md](reports/releases/2026-03-14-release-note.md)
14. 최신 전체 검증 확인: [2026-03-16-validation-checkpoint.md](reports/checkpoints/2026-03-16-validation-checkpoint.md)
15. 이전 최종 점검 요약 확인: [final-checkpoint.md](reports/checkpoints/final-checkpoint.md)
