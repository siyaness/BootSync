# BootSync Project Plan

## 목표

BootSync를 포트폴리오용 데모가 아니라, 실제 사용자가 매일 열어보는 서비스 수준으로 완성하고 운영 가능한 상태까지 끌어올린다.

## 현재 기준

이미 완료된 범위:

- `Member` 기반 로그인과 세션 보안 기본선
- custom principal에 `memberId`, `username`, `displayName` 반영
- safe markdown 렌더링
- 출결 수정/삭제 엔드포인트와 화면 흐름 정리
- 스니펫 삭제
- 설정의 기본 프로필 수정
- 설정의 비밀번호 변경
- 계정 삭제 요청 등록과 `PENDING_DELETE` 전환
- 삭제 유예 기간 내 운영자 수동 취소 서비스
- `PENDING_DELETE` 계정 purge 서비스와 선택적 스케줄 잡
- 회원가입 후 자동 로그인
- recovery email signup verification
- settings 기반 recovery email change / verify / resend
- signup/login/recovery-email 관련 rate limit 기본선
- 대시보드, 출결 조회/입력, 스니펫 조회/검색/작성/수정
- 스니펫 secret warning 흐름
- 사용자 스코프 소유권 검사 기본선
- 통합 웹 테스트 기반 회귀 검증

## 남은 큰 단계

### Phase 1. 명세 핵심 갭 마감

상태: 완료

완료 항목:

- 핵심 화면 기능의 세션 사용자 기준 동작 정리
- custom principal, safe markdown, 출결/스니펫 CRUD, 설정 프로필/비밀번호 반영
- 삭제/수정/권한 관련 회귀 테스트 보강

### Phase 2. 계정 생명주기와 운영 기능

목표: 실제 서비스 운영에 필요한 개인정보/계정 관리 흐름을 닫는다.

상태: 진행 중

완료된 문서화:

- 삭제 요청 등록부와 취소 runbook
- recovery email 기반 운영자 보조 복구 절차 runbook
- 로컬 운영 rehearsal 보고서 (`purge`, password reset, 복원 후 scrub)

완료된 구현 보강:

- 운영자 보조 삭제 취소 maintenance runner
- 운영자 보조 비밀번호 초기화 maintenance runner
- purge one-shot rehearsal runner와 처리 건수 로그
- 로컬 rehearsal seed runner와 실행 스크립트

남은 우선순위:

1. purge 스케줄 운영 첫 실행 기록 확보

완료 조건:

- 삭제 요청부터 purge 대상 선정까지 코드 또는 운영 절차가 존재한다.
- 상태 변경 시 세션 종료 규칙이 보장된다.
- 운영자가 따라야 할 절차 문서가 준비된다.
- 로컬 rehearsal 증적이 있고, 운영 첫 실행 기록만 별도 후속으로 남는다.

### Phase 3. 운영 준비

목표: 배포 가능한 서비스 수준으로 환경과 문서를 준비한다.

진행 상황:

- 로컬 backup/restore automation script와 rehearsal report는 추가됐다.
- 개인정보 처리방침, 이용약관, 삭제/복구 절차 공개용 초안이 추가됐다.
- `EC2 + k3s` 기준으로 바로 시작할 수 있는 `k8s/` 초안 매니페스트가 추가됐다.
- 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치는 아직 남아 있다.
- `RTO 8시간`은 prod-like 복원 리허설 기준으로 별도 실측이 필요하다.

우선순위:

1. 운영 SMTP 실메일 스모크 테스트 수행
2. AWS 실제 배포 시작 (`ECR -> RDS -> EC2/k3s`)
3. 운영 AWS 자격증명 기준 S3 업로드 1회와 일일 스케줄 배치
4. prod-like 복원 리허설 수행과 `RTO 8시간` 실측
5. 정책 문서 초안 확정과 실제 공개값(운영 주체/문의처/도메인) 반영

완료 조건:

- 로컬뿐 아니라 운영 환경 가정에서도 recovery email이 실제로 발송된다.
- 백업/복구 절차와 스크립트가 있고, 최소 1회 운영 또는 prod-like 복원 기록이 남아 있다.
- 개인정보/운영 문서 초안이 준비되고, 공개용 운영 값까지 채워진다.

## 작업 순서 가이드

다음 작업은 가급적 아래 순서로 진행한다.

1. deployment (`docker build -> ECR push -> RDS -> EC2/k3s`)
2. operations evidence (`SMTP smoke test`, `purge first scheduled run`, `S3 upload + daily schedule`, `prod-like restore + RTO`)

## 매 작업 시 체크

- 이 작업이 명세 어느 항목을 닫는가
- 소유권, 인증, 세션, rate limit에 영향이 있는가
- README와 SPEC_TRACKER를 같이 업데이트해야 하는가
- 관련 테스트를 무엇으로 증명할 것인가
