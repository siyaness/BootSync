# BootSync Agent Guide

이 프로젝트에서 작업을 시작할 때는 아래 순서를 기본으로 따른다.

## 시작 전 필수 확인

1. [BOOTSYNC_SPEC_V2.md](docs/spec/BOOTSYNC_SPEC_V2.md)를 확인해 현재 작업이 명세와 어떻게 연결되는지 파악한다.
2. [PROJECT_PLAN.md](docs/planning/PROJECT_PLAN.md)를 확인해 현재 단계와 다음 우선순위를 파악한다.
3. [SPEC_TRACKER.md](docs/spec/SPEC_TRACKER.md)를 확인해 구현 상태와 남은 갭을 파악한다.
4. 작업 내용이 실행 방법이나 사용자 흐름에 영향을 주면 [README.md](README.md)도 함께 확인한다.
5. 문서 관련 작업이면 [docs/README.md](docs/README.md)부터 열어 현재 문서 구조와 연결 문서를 확인한다.

## 우선순위 원칙

우선순위는 항상 아래 순서를 따른다.

1. 명세의 보안, 인증, 권한, 데이터 무결성 규칙
2. 사용자 데이터 격리와 소유권 보장
3. 테스트 가능성과 회귀 방지
4. 실제 화면 동작과 UX
5. 문서와 운영 준비

## 작업 원칙

- 명세와 충돌하는 구현이 보이면 편의보다 명세를 우선한다.
- `memberId`, 계정 상태, 소유권, recovery email verification, rate limit 같은 핵심 규칙은 임의로 완화하지 않는다.
- 일반 사용자 요청에서 `memberId`를 입력으로 받지 않는다.
- 소유권이 필요한 조회는 가능하면 리포지토리 쿼리 단계에서 `memberId` 조건을 포함한다.
- 화면만 고치지 말고, 관련 서비스/저장소/테스트까지 함께 맞춘다.
- 동작이 바뀌면 README와 추적 문서도 함께 업데이트한다.

## 검증 원칙

- 수정 후에는 최소 `compileJava`, `compileTestJava`, 관련 테스트를 확인한다.
- 보안, 인증, 권한, 출결, 스니펫, recovery email 흐름을 건드렸다면 가능하면 `.\gradlew.bat test` 전체를 돌린다.
- 테스트가 실패하면 그 실패를 입력으로 삼아 즉시 수정하고 다시 검증한다.

## 완료 기준

작업은 아래를 만족할 때 완료로 본다.

- 명세와 충돌하지 않는다.
- 관련 테스트가 통과한다.
- README 또는 추적 문서가 현재 상태와 맞는다.
- 남은 리스크가 있으면 응답에서 분명히 적는다.

## 문서 역할

- [BOOTSYNC_SPEC_V2.md](docs/spec/BOOTSYNC_SPEC_V2.md): 최종 기준 명세
- [PROJECT_PLAN.md](docs/planning/PROJECT_PLAN.md): 전체 개발 로드맵과 현재 우선순위
- [SPEC_TRACKER.md](docs/spec/SPEC_TRACKER.md): 명세 항목별 구현 상태 추적표
- [README.md](README.md): 실행 방법과 현재 사용자 관점 동작
- [docs/README.md](docs/README.md): 문서 허브와 세부 문서 진입점
