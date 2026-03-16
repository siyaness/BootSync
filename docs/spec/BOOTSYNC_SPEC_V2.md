# BootSync 개발 명세서 v2

## 1. 프로젝트 개요

- 프로젝트명: BootSync
- 서비스 유형: 대한민국 국비지원 IT 교육과정 훈련생을 위한 웹 서비스
- 핵심 가치: 출결 확인, 월 장려금 예상 계산, 학습 스니펫 정리를 한 곳에서 처리할 수 있는 서비스
- 1차 목표: 포트폴리오용 데모가 아니라 실제 사용자가 매일 열어보는 서비스 만들기
- 배포 대상: AWS 단일 클라우드

## 2. 제품 방향

### 2.1 제품 원칙

- 기능 수보다 실사용성을 우선한다
- 모바일과 데스크톱을 모두 주요 사용 환경으로 본다
- 작은 화면에서도 빠르게 쓸 수 있어야 하고, 큰 화면에서는 더 높은 정보 밀도를 제공해야 한다
- 복잡한 대시보드보다 빠른 입력 경험을 우선한다
- 유행하는 구조보다 완성 가능한 구조를 택한다
- 멀티클라우드 과시보다 안정적인 단일 배포를 우선한다

### 2.2 이 서비스가 필요한 이유

훈련생은 반복적으로 다음 문제를 겪는다.

- 현재 월 출결 상태를 한눈에 파악하기 어렵다
- 결석, 지각, 조퇴가 장려금에 어떤 영향을 주는지 즉시 알기 어렵다
- 배운 명령어, 에러 해결 기록, 설치 절차가 여러 메모와 채팅에 흩어진다
- 이동 중이나 쉬는 시간에 모바일로 간단히 확인하고 입력하고 싶다

BootSync는 이 문제를 하나의 일상형 서비스로 해결한다.

## 3. 타깃 사용자

### 3.1 주요 사용자

- 대한민국 국비지원 IT 교육과정 훈련생
- 현재 목표 학원과 유사한 출결/장려금 규칙을 가진 교육과정 수강생

### 3.2 주요 문제

- 출결과 장려금 변동을 추적하기 어렵다
- 학습 내용을 체계적으로 정리하기 어렵다
- 예전에 해결한 명령어와 설정을 다시 찾기 어렵다
- 복잡한 서비스는 모바일에서 사용하기 불편하다

## 4. MVP 범위

### 4.1 v1 포함 기능

- 아이디 기반 회원가입 및 로그인
- 개인 대시보드
- 날짜별 출결 입력
- 월별 출결 요약
- 고정 규칙 기반 장려금 예상 계산
- 스니펫 작성, 수정, 삭제, 검색, 태그 필터링
- 모바일 반응형 UI
- AWS 배포
- S3 기반 일일 DB 백업

### 4.2 v1 제외 기능

- 팀 협업 기능
- 소셜 로그인
- 푸시 알림
- 관리자 콘솔
- 공개 커뮤니티 게시판
- AI 요약 기능
- 스니펫 고정(pin) 기능
- 다중 인스턴스 확장
- Kubernetes를 제품 기능의 필수 출시 범위로 두지 않는다

정리:

- v1의 `AWS 배포` 준비 과정에서 `EC2 + k3s` 수준의 단일 클라우드 운영 매니페스트, 모니터링, GitOps 템플릿을 저장소에 포함하는 것은 허용한다.
- 이는 사용자 기능을 늘리는 범위가 아니라 배포/운영 준비 범위로 본다.

### 4.3 개발 / 출시 단계 구분

- `로컬 개발 완료`: Phase 1~3 핵심 기능이 로컬에서 동작하고, `26.1 프로젝트 스캐폴딩 완료 기준`을 만족한 상태
- `비공개 테스트 가능`: 소규모 사용자 테스트에 필요한 인증, 출결, 스니펫, 시크릿 경고, 기본 보안 설정, 백업 작업이 동작하는 상태
- `공개 출시 가능`: HTTPS, recovery email 실발송, 백업/복원 리허설, 삭제 요청 운영 절차, 개인정보/이용약관 문서, 기본 모니터링까지 갖춘 상태
- 본 문서의 구현 단계와 수용 기준은 위 3단계를 순차적으로 통과하는 방식으로 해석한다

## 5. 핵심 비즈니스 규칙

### 5.1 장려금 계산 규칙

- 장려금 예상 계산은 `개강일 기준 1개월 단위기간`을 기준으로 한다
- 기본 규칙 프로필의 `1일 지급액`은 15,800원이다
- 단위기간당 `지급 반영 상한`은 20일이다
- 예상 장려금은 `1일 지급액 × 지급 반영 일수`로 계산한다
- `지급 반영 일수`는 `인정 출석일수`를 20일 상한에 맞춰 자른 값이다
- `인정 출석일수`는 저장된 출결 기준으로 계산하며, 미입력 날짜를 시스템이 자동 결석 처리하지 않는다
- 지각 3회는 결석 1회로 환산한다
- 조퇴 3회도 결석 1회로 환산한다
- 최종 장려금은 0원 미만으로 내려가지 않는다

적용 범위:

- v1의 공식 지원 장려금 규칙은 현재 목표 학원/과정의 1개 기본 프로필(`15,800원 × 20일 상한`)을 포함하되, 사용자별 과정 정보에서 `1일 지급액`과 `지급 상한 일수`를 수정할 수 있다
- 다른 학원, 다른 기수, 다른 과정은 지급 규칙이 다를 수 있으므로 화면에는 반드시 `예상 금액`임을 표시한다
- 실제 지급 기준이 학원 공지와 다를 경우 학원 공지를 우선한다

### 5.2 데이터 격리 규칙

- 사용자는 자신의 출결, 스니펫, 설정 정보만 조회 및 수정할 수 있다
- 모든 목록/상세/요약 조회는 인증된 사용자 id 기준으로 필터링되어야 한다
- 로그인 이후 첫 진입 화면은 해당 사용자 세션 기준의 개인 대시보드여야 한다
- 개인 화면에는 다른 사용자의 이름, 출결 수치, 스니펫, 식별자, 통계가 섞여서 노출되면 안 된다
- 다른 사용자의 리소스를 URL 추측이나 요청 변조로 조회하려는 경우 `403` 또는 `404`로 차단하고, 존재 여부를 불필요하게 노출하지 않는다
- v1의 일반 사용자 화면 라우트는 공개 프로필 개념이 아니라 `현재 로그인한 사용자 컨텍스트` 기준으로 동작한다

### 5.3 날짜 처리 규칙

- 서비스 기준 시간대는 `Asia/Seoul`이다
- 출결은 UTC 시각이 아니라 `KST(Asia/Seoul) 기준 LocalDate`로 저장한다
- 월별 요약은 `yyyy-MM` 기준으로 계산한다
- v1에서는 업무 데이터와 감사성 시각(`created_at`, `updated_at`) 모두 `KST(Asia/Seoul)` 기준으로 저장하고 해석한다
- 출결 날짜 계산 시 JVM 기본 시간대나 DB 세션 시간대를 사용하지 않고, 항상 애플리케이션에서 `ZoneId.of("Asia/Seoul")`를 명시적으로 사용한다

### 5.4 출결 입력 및 장려금 계산 경계 규칙

- 장려금 차감 계산에서 `지각`과 `조퇴`는 서로 합산하지 않고 각각 별도 묶음으로 계산한다
- 예를 들어 `지각 2회 + 조퇴 1회`는 v1에서 결석 1회로 환산하지 않는다
- `지각 4회`는 결석 1회 + 지각 1회 잔여, `조퇴 5회`는 결석 1회 + 조퇴 2회 잔여로 해석한다
- 저장되지 않은 날짜를 시스템이 자동으로 `결석` 처리하지 않는다
- 주말, 공휴일, 휴강일, 조기종강일 등 실제 수업일 판단은 v1에서 시스템이 자동 계산하지 않고, 사용자가 명시적으로 저장한 기록만 집계한다
- v1에서는 미래 날짜 출결 입력을 허용하지 않는다
- 동일 날짜에 대한 중복 저장 또는 브라우저 이중 제출이 발생해도 사용자에게는 검증 오류 또는 이미 처리된 요청 안내를 보여주고, 서버 `500`으로 끝나면 안 된다
- 출결 수정/삭제 직후 월 요약과 예상 장려금은 즉시 재계산되어야 한다

## 6. 핵심 사용자 흐름

### 6.1 로그인 후 개인 화면 진입 흐름

1. 사용자가 아이디와 비밀번호로 로그인한다
2. 서버는 세션에 사용자 식별 정보를 저장한다
3. 사용자는 자신의 대시보드로 이동한다
4. 대시보드에는 로그인한 사용자 기준의 오늘 출결, 이번 달 요약, 예상 장려금, 최근 스니펫만 표시된다
5. 데이터가 없는 경우에도 다른 사용자 데이터 대신 개인용 빈 상태 화면을 보여준다

### 6.2 일일 출결 입력 흐름

1. 사용자가 대시보드를 연다
2. 오늘 출결 카드와 이번 달 요약을 본다
3. 오늘 상태를 `출석`, `지각`, `조퇴`, `결석` 중 하나로 입력하고, 필요하면 짧은 개인 메모를 남긴다
4. 대시보드가 즉시 이번 달 예상 차감액과 예상 장려금을 갱신한다

### 6.3 스니펫 보관 흐름

1. 사용자가 스니펫 페이지로 이동한다
2. 제목, 내용, 태그를 입력한다
3. 저장한다
4. 나중에 키워드나 태그로 다시 찾는다

### 6.4 월별 점검 흐름

1. 사용자가 월간 출결 화면을 연다
2. 출결 통계를 확인한다
3. 예상 장려금과 차감 사유를 본다
4. 잘못 입력된 날짜가 있으면 수정한다

## 7. 화면 요구사항

### 7.1 로그인 / 회원가입

- 모바일과 데스크톱 모두 자연스러운 반응형 레이아웃
- 빠른 폼 제출
- 필드별 검증 메시지 표시
- 회원가입 폼에는 `username`, `password`, `display_name`, `recovery_email` 입력 필드가 포함된다
- 회원가입 성공 시 계정을 생성한 뒤 자동 로그인하고 대시보드로 이동한다
- 회원가입 직후에는 대시보드 또는 설정 화면에서 복구용 이메일 인증 안내와 재발송 동선을 지속적으로 제공한다
- 이메일 인증 링크에 진입하면 바로 소비하지 않고, 확인 화면에서 사용자가 명시적으로 인증을 확정하는 흐름을 사용한다

### 7.2 대시보드

- 오늘 출결 카드
- 이번 달 요약 카드
- 예상 장려금 카드
- 최근 스니펫 카드
- 빠른 액션 버튼
- 모든 카드와 통계는 로그인한 사용자 본인 데이터만 기준으로 렌더링한다
- 사용자의 이름 또는 표시명을 상단에 명확히 보여 현재 개인 화면임을 인지할 수 있어야 한다
- 출결 데이터가 아직 없는 신규 사용자는 개인화된 빈 상태 안내와 첫 입력 유도 버튼을 본다
- 데스크톱에서는 2열 또는 3열 카드 레이아웃 제공
- 데스크톱에서는 출결 요약과 최근 스니펫을 한 화면에서 함께 확인 가능해야 한다

### 7.3 출결 캘린더

- 월간 캘린더 뷰
- 날짜별 출결 상태 배지
- 수정용 모달 또는 슬라이드 패널
- 수정 패널에는 선택 입력으로 짧은 개인 메모를 남길 수 있다
- 출결 메모는 개인 참고용이며 월 통계와 예상 장려금 계산에는 영향을 주지 않는다
- 출결 메모는 기본 캘린더 격자에 항상 노출하지 않고, 수정 패널 또는 상세 확인 시에만 보여줄 수 있다
- 상단 월별 통계
- 데스크톱에서는 월간 캘린더와 월별 통계를 동시에 볼 수 있어야 한다

### 7.4 스니펫 목록

- 검색 입력창
- 태그 필터 칩
- v1의 태그 필터는 단일 태그 정확 일치만 지원한다
- 기본 정렬은 최신 수정순이다
- 비어 있을 때 작성 유도 문구 표시
- 데스크톱에서는 목록 영역과 필터 영역의 시선 이동이 적도록 배치한다

### 7.5 스니펫 상세

- Markdown 렌더링
- 태그 목록
- 수정 / 삭제 버튼
- v1.1에서 연관 태그 스니펫 추천 추가

## 8. UX / UI 원칙

- 360px 모바일부터 1440px 데스크톱까지 자연스럽게 대응하는 반응형 설계
- 모바일은 빠른 입력과 확인에 최적화하고, 데스크톱은 정보 탐색과 정리에 최적화한다
- 데스크톱 화면에서도 빈 공간이 과도하게 남지 않도록 정보 밀도와 레이아웃 균형을 맞춘다
- 핵심 행동은 3탭 이내로 도달 가능해야 한다
- 톤은 차분하고 실용적이어야 하며 과한 “관리자 대시보드 느낌”은 피한다
- 가독성 높은 타이포그래피와 여백을 확보한다
- 필요한 곳에만 최소한의 인터랙션을 둔다

## 9. 기능 요구사항

### 9.1 인증

- 로컬 계정 회원가입
- 세션 기반 로그인
- BCrypt 기반 비밀번호 암호화
- 로그아웃
- Spring Security 기반 보호 라우트
- 복구용 이메일 수집 및 인증 메일 발송
- 회원가입 직후와 복구용 이메일 변경 요청 시, `30분` TTL의 1회용 verification token을 별도 저장 모델에 발급한다
- 회원가입 시 입력한 `recovery_email`은 검증 성공 전까지 `member.recovery_email`에 최종 반영하지 않고, pending verification target으로만 취급한다
- `ACTIVE` 상태 계정만 로그인 가능
- `recovery_email` 미검증 상태 자체는 일반 로그인 차단 사유가 아니며, 원격 비밀번호 초기화와 원격 삭제 요청만 제한한다
- 검증된 `recovery_email`만 원격 비밀번호 초기화와 원격 삭제 요청의 본인 확인 수단으로 인정한다
- 비밀번호 분실 사용자를 위한 운영자 수동 비밀번호 초기화 절차를 문서화한다
- 인증되지 않은 사용자가 보호 화면에 접근하면 로그인 화면으로 이동한다
- 로그인 성공 후에는 원래 요청한 보호 화면 또는 기본 대시보드로 이동한다

설계 메모:

- v1은 동일 출처 웹앱이므로 세션 쿠키 방식이 더 적합하다
- JWT는 v1에서 구조를 복잡하게 만들 뿐 실익이 크지 않아 제외한다
- `recovery_email`은 로그인 아이디가 아니라 계정 복구 및 민감 작업 본인 확인용 필드다
- 회원가입 성공 시 자동 로그인 후 대시보드로 이동하고, 미검증 recovery email 안내는 배너 또는 설정 화면에서 지속 노출한다
- recovery email 검증/변경 token은 클라이언트나 세션 임시값만으로 처리하지 않고, 서버 저장 모델에서 발급/무효화/소비 상태를 관리한다
- 미검증 recovery email target은 전역 unique 자원을 선점하지 않으며, 검증 성공 시점에만 최종 `member.recovery_email`로 승격한다
- 이메일 링크 기반 셀프서비스 비밀번호 재설정은 v1.1 이후 도입 여부를 검토한다
- v1의 원격 비밀번호 초기화/삭제 요청은 자동 self-service가 아니라, 검증된 `recovery_email` 확인을 거친 운영자 보조 절차로 처리한다

### 9.2 출결

- 날짜별 출결 등록
- 출결 수정
- 출결 삭제
- 선택 입력의 짧은 개인 메모 저장
- 동일 사용자 + 동일 날짜 중복 입력 방지
- 미래 날짜 등록 금지
- 저장되지 않은 날짜 자동 결석 처리 금지
- 동일 날짜 이중 제출 시 비즈니스 오류 또는 멱등 처리로 마무리하고 `500`을 내지 않는다
- 월별 출결 요약 조회
- 캘린더용 월별 데이터 조회

### 9.3 장려금 요약

- `출석`, `지각`, `조퇴`, `결석` 개수 집계
- 지각/조퇴를 결석 환산치로 계산
- 예상 장려금 계산
- 화면에 계산 규칙 설명 표시
- 화면에 `예상 금액이며 학원 공지가 최종 기준`이라는 안내 문구 표시

### 9.4 스니펫

- 스니펫 작성
- 스니펫 수정
- 스니펫 삭제
- 제목/내용 기준 검색
- 태그 필터링
- v1의 검색 API는 `q`와 단일 `tag` 필터만 지원한다
- `q`와 `tag`를 함께 사용할 수 있으며, 결과 정렬은 최신 수정순으로 고정한다
- 다중 태그 조합, 사용자 지정 정렬, 페이지네이션은 v1.1 이후 검토한다
- 최근 스니펫 목록 제공
- 안전한 Markdown 렌더링
- 작성/수정 화면에 `비밀번호, API 키, 개인 토큰을 저장하지 말 것`이라는 경고 문구를 표시한다
- AWS Access Key, JWT, `BEGIN PRIVATE KEY` 등 고위험 시크릿 패턴이 감지되면 1차 저장은 중단하고 경고를 띄운다
- 사용자가 경고를 확인하고, 서버가 직전 경고 응답에서 발급한 유효한 `secretWarningToken`과 함께 다시 제출한 경우에만 저장을 허용한다

### 9.5 설정

- 기본 프로필 수정
- 비밀번호 변경
- 복구용 이메일 인증 상태 확인
- 복구용 이메일 변경
- 로그인 사용자는 `POST /api/settings/recovery-email/resend`를 통해 현재 계정의 pending verification target에 대해서만 인증 메일을 재발송할 수 있다
- 복구용 이메일 변경은 새 이메일 검증 완료 전까지 기존 `recovery_email`을 유지한다
- 시간대는 v1에서 `Asia/Seoul` 고정

### 9.6 사용자별 화면 / 권한

- `/dashboard`, `/attendance`, `/snippets`, `/settings`는 모두 로그인한 사용자 전용 화면이다
- 화면 렌더링과 API 응답은 요청 파라미터보다 서버 세션의 사용자 식별 정보를 우선 기준으로 삼는다
- 스니펫 상세, 수정, 삭제는 리소스 소유권 검사를 반드시 통과해야 한다
- 출결 수정과 삭제도 본인 기록에 대해서만 허용한다
- v1 일반 사용자 기능에서는 다른 사용자의 페이지를 조회하는 공개 URL 패턴을 두지 않는다
- 사용자마다 별도 HTML 파일을 생성하지 않고, 공통 React 라우트(`/app/...`)와 공통 same-origin API를 사용하되 세션 사용자 기준으로 데이터만 달라지게 구현한다

### 9.7 계정 생명주기 / 개인정보

- v1 회원가입 시 수집하는 필드는 `username`, `password`, `display_name`, `recovery_email`로 최소화한다
- `recovery_email`은 로그인 식별자가 아니라 계정 복구 및 민감 작업 본인 확인용으로만 사용한다
- 전화번호, 실명, 주민등록번호, 출결 증빙 이미지 같은 추가 개인정보는 v1에서 수집하지 않는다
- 공개 출시 전 `개인정보 처리방침`, `서비스 이용약관`, `운영자 수동 비밀번호 초기화 절차`, `탈퇴/삭제 처리 절차`를 문서로 확정해야 한다
- 회원가입 직후 발송한 이메일 인증이 완료되기 전까지는 `recovery_email`을 신뢰 가능한 복구 수단으로 보지 않는다
- v1에는 셀프 회원탈퇴 UI가 없더라도, 검증된 `recovery_email` 확인 또는 동등하게 강한 대면 확인을 거친 뒤 운영자가 삭제 요청을 처리하는 절차는 반드시 있어야 한다
- 검증된 `recovery_email`이 없는 계정은 원격 비밀번호 초기화와 원격 삭제 요청 대상에서 제외하고, 대면 확인 같은 대체 절차가 없으면 처리하지 않는다
- 삭제 요청이 접수되면 해당 계정 상태를 즉시 `PENDING_DELETE`로 바꾸고, 운영 DB의 본 데이터는 `7일 이내` 영구 삭제를 목표로 한다
- 삭제 요청 처리 시 기존 활성 세션은 즉시 만료 대상으로 표시하고, 보호된 모든 요청에서 현재 계정 상태를 재검사해 `ACTIVE`가 아니면 세션을 즉시 무효화한다
- 삭제 요청을 받은 계정에는 `delete_requested_at`, `delete_due_at`를 기록해 운영 유예 기간과 실제 삭제 시점을 추적한다
- 삭제 요청은 `delete_due_at` 이전까지 운영자 수동 취소가 가능하며, 취소 시 `status`를 `ACTIVE`로 되돌리고 `delete_requested_at`, `delete_due_at`를 비운 뒤 새 로그인만 허용한다
- 삭제 취소는 삭제 요청과 같거나 더 강한 본인 확인 절차를 통과한 경우에만 수행하고, 취소 사유와 수행 기록을 `삭제 요청 등록부` 또는 동등한 보안 운영 티켓에 남긴다
- 실제 hard delete는 `AccountDeletionPurgeJob` 또는 동등한 전용 purge 책임에서 수행한다
- purge 대상은 `status = PENDING_DELETE` 이고 `delete_due_at <= now` 조건을 만족하는 계정이다
- purge는 최소 하루 1회 수행하거나, 동등한 운영자 수동 purge 절차를 문서화한다
- v1의 기본 purge 순서는 `snippet_tag -> snippet -> tag -> attendance_record -> recovery_email_verification_token -> attendance_audit_log 직접 식별 참조 정리 또는 비식별화 -> member`로 두며, 이미 일부 단계가 끝난 상태에서도 재실행 가능해야 한다
- 백업에는 삭제 전 데이터가 남을 수 있으므로, 백업 라이프사이클 만료 시점까지 최대 `35일` 범위에서 잔존할 수 있음을 정책 문서에 명시한다
- v1에서는 account deletion ledger, tombstone, reconciliation 자동화를 도입하지 않는다
- 대신 운영자는 앱 외부의 접근 통제된 `삭제 요청 등록부`(예: 보안 문서 또는 운영 티켓)에 `member_id`, `username`, `delete_requested_at`, `delete_due_at`, 처리 상태를 남기고, 최소 마지막 관련 백업 만료 시점까지 보존하며 백업 복원 시 수동 scrub의 근거로 사용한다
- 백업 복원 후 서비스 재오픈 전에는 `삭제 요청 등록부`를 기준으로 삭제 요청 계정을 수동 scrub 해야 한다
- 운영자 수동 비밀번호 초기화와 삭제 요청은 접근 통제된 운영 메일함 또는 보안 운영 티켓에서만 접수한다
- 원격 비밀번호 초기화와 삭제 요청의 최소 본인 확인 수단은 `검증된 recovery_email` 주소로 보낸 확인 메시지에 대한 통제 증명이고, 검증된 recovery email이 없으면 원격 처리를 하지 않는다
- 운영자 수동 고위험 조치 기록에는 최소 `request_type`, `member_id`, `username`, `request_channel`, `identity_verification_basis`, `reviewed_by`, `executed_by`, `executed_at`, `outcome`, `reason`을 남긴다
- v1은 단일 운영자 환경을 허용하므로 이중 승인 필수는 두지 않지만, 고위험 조치의 승인자/수행자가 동일한 경우 그 사실을 기록에 남긴다

## 10. 비기능 요구사항

- 일반적인 페이지 로드 기준 P95 응답속도 500ms 이하 목표
- 시맨틱 HTML과 키보드 포커스 기반 기본 접근성 확보
- 일일 백업 수행
- 운영 환경 HTTPS 필수
- 비밀번호 평문 저장 금지
- 로그에 비밀번호, 세션 시크릿 노출 금지
- 백업/복구 목표는 v1 기준 `RPO 24시간`, `RTO 8시간` 이내를 목표로 한다
- 운영 로그와 보안 로그의 기본 보존기간은 `30일`로 두고, 장애 대응이나 법적 이슈로 연장할 때만 별도 근거를 남긴다
- 백업 실패, 디스크 부족, 앱 비정상 종료 같은 운영 이벤트는 당일 안에 감지 가능해야 한다

## 11. 권장 기술 스택

### 11.1 애플리케이션

- Java 17
- Gradle 8
- Spring Boot 3.x
- Spring MVC
- Spring Security
- Spring Validation
- Spring Data JPA
- React 18
- TypeScript
- Vite
- React Router
- Tailwind CSS
- MySQL 8
- Flyway
- Spring Boot Actuator

### 11.2 배포

- Docker
- Docker Compose
- Nginx
- AWS EC2
- AWS S3
- AWS SES
- Route 53

### 11.3 이 스택을 쓰는 이유

- 기존 Spring 역량을 그대로 활용할 수 있다
- 별도 SPA 분리 없이도 프론트 완성도를 높일 수 있다
- 혼자서 끝까지 구현하고 운영하기 쉽다
- 비용과 복잡도를 낮출 수 있다

### 11.4 DB 안정성 원칙

- v1의 공식 지원 DB는 `MySQL 8` 단일 환경으로 고정한다
- 스토리지 엔진은 모든 테이블에서 `InnoDB`를 사용한다
- 문자셋은 `utf8mb4`를 사용한다
- 스키마 변경은 수동 변경이 아니라 `Flyway` 마이그레이션으로만 수행한다
- 애플리케이션, DB 세션, 비즈니스 날짜, 감사 시각 모두 `KST(Asia/Seoul)` 기준으로 일관되게 처리한다
- 서버, 컨테이너, DB 세션 시간대가 섞이지 않도록 운영 환경에서도 `Asia/Seoul`을 명시적으로 고정한다
- `member.status`, `attendance_audit_log.action`처럼 변화 가능성이 낮은 내부 상태는 DB 제약으로 관리하고, 출결 상태 허용 값 검증은 애플리케이션 계층에서 수행한다

## 12. 아키텍처 결정

### 12.1 채택 구조

- 모듈형 모놀리식
- Spring Boot 애플리케이션 1개
- MySQL DB 1개
- Nginx 리버스 프록시 1개
- EC2 인스턴스 1대로 v1 운영

### 12.2 제외 구조

- React + 별도 API 서버 분리
- MSA
- Kubernetes
- 멀티리전 / 멀티클라우드

제외 이유:

- 현재 단계에서 배포 복잡도, 운영 리스크, 개발 범위를 동시에 증가시킨다

### 12.3 개인화 화면 처리 방식

- 로그인한 모든 사용자는 동일한 `/app` URL 구조와 동일한 React 화면 구성을 사용한다
- 예를 들어 `/app/dashboard`는 사용자마다 다른 경로가 아니라 공통 경로 1개만 둔다
- 서버는 세션 또는 Spring Security Principal에서 현재 로그인 사용자 id를 읽는다
- 서비스 계층과 리포지토리는 이 사용자 id를 기준으로 출결, 스니펫, 요약 데이터를 조회한다
- 서버는 공통 프론트 셸(`/app`)과 JSON API를 제공하고, 프론트는 공통 라우트와 상태 저장소를 통해 사용자별 데이터를 표시한다
- 즉, 경로와 화면 구조는 공유하고 데이터만 사용자별로 달라지는 구조를 채택한다
- 일반 사용자 기능에서는 클라이언트가 임의의 `memberId`를 보내 화면 대상을 바꾸는 방식을 허용하지 않는다
- 이 방식은 Gmail, 은행, 쇼핑몰 마이페이지처럼 현업 웹서비스에서 가장 흔한 개인화 처리 방식이다

## 13. 도메인 모델

### 13.1 Member

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `username`: `VARCHAR(20)` NOT NULL UNIQUE
- `password_hash`: `VARCHAR(255)` NOT NULL
- `display_name`: `VARCHAR(20)` NOT NULL
- `recovery_email`: `VARCHAR(255)` NULL UNIQUE
- `recovery_email_verified_at`: `DATETIME(6)` NULL
- `status`: `ENUM('ACTIVE','PENDING_DELETE','DISABLED')` NOT NULL DEFAULT 'ACTIVE'
- `delete_requested_at`: `DATETIME(6)` NULL
- `delete_due_at`: `DATETIME(6)` NULL
- `created_at`: `DATETIME(6)` NOT NULL
- `updated_at`: `DATETIME(6)` NOT NULL

규칙:

- 인증은 `status = ACTIVE` 계정만 허용한다
- 로그인 식별자는 항상 `username`이고, `recovery_email`은 복구 및 본인 확인용으로만 사용한다
- `recovery_email`은 소문자화와 trim 같은 정규화를 거쳐 저장한다
- `member.recovery_email`은 검증이 완료된 최종 recovery email만 저장하며, 미검증 target은 이 컬럼에 저장하지 않는다
- `recovery_email_verified_at`가 채워진 이메일만 원격 비밀번호 초기화와 원격 삭제 요청 확인 수단으로 인정한다
- 복구용 이메일 변경 요청이 시작돼도 검증 성공 전까지 기존 `recovery_email`과 `recovery_email_verified_at`는 유지한다
- 회원가입 직후 아직 검증되지 않은 계정은 `recovery_email = NULL`, `recovery_email_verified_at = NULL` 상태를 가질 수 있다
- 미검증 recovery email target은 전역 unique 자원을 선점하지 않으며, 검증 성공 시점에 `member.recovery_email` unique 제약을 다시 확인한 뒤에만 승격한다
- 삭제 요청이 접수된 계정은 즉시 `PENDING_DELETE` 또는 동등한 비활성 상태로 전환한다
- 삭제 요청 시 `delete_requested_at`와 `delete_due_at`를 함께 기록하고, `delete_due_at`는 기본적으로 `delete_requested_at + 7일`로 계산한다
- 실제 hard delete가 끝나면 해당 계정의 본 데이터와 함께 관련 필드는 더 이상 남기지 않는다

### 13.2 RecoveryEmailVerificationToken

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `member_id`: `BIGINT UNSIGNED` NOT NULL
- `purpose`: `ENUM('SIGNUP_VERIFY','RECOVERY_EMAIL_CHANGE')` NOT NULL
- `target_email`: `VARCHAR(255)` NOT NULL
- `token_hash`: `CHAR(64)` NOT NULL
- `issued_at`: `DATETIME(6)` NOT NULL
- `expires_at`: `DATETIME(6)` NOT NULL
- `consumed_at`: `DATETIME(6)` NULL
- `invalidated_at`: `DATETIME(6)` NULL

제약 조건:

- `foreign key(member_id) references member(id)`

규칙:

- recovery email 검증 token 원문은 저장하지 않고 hash만 저장한다
- `target_email`은 소문자화와 trim 같은 정규화를 거친 뒤 저장하며, 검증 대상 이메일 값과 정확히 바인딩한다
- token은 `30분` TTL의 1회용이며, `expires_at` 이후에는 인정하지 않는다
- `GET` 확인 화면 진입은 token을 소비하지 않고, `POST` 확인 요청에서만 `consumed_at`을 기록하고 실제 상태를 변경한다
- 같은 계정과 같은 `purpose`에 대해 새 token을 발급하면 기존 미사용 token은 `invalidated_at`으로 무효화한다
- 회원가입 직후 이메일 검증과 설정 화면의 recovery email 변경 검증은 같은 저장 모델을 사용하되 `purpose`로 구분한다
- 같은 계정과 같은 `purpose`에서 가장 최근 미소비 token의 `target_email`을 해당 검증 흐름의 현재 pending target으로 본다
- recovery email 변경 검증이 성공하기 전까지 기존 `member.recovery_email`은 유지하고, 성공 시점에만 `target_email`을 `member.recovery_email`로 반영한다
- `target_email`은 pending verification target이며, 자체로는 전역 unique를 차지하지 않는다
- token 소비 시점에는 `target_email`이 아직 어떤 `member.recovery_email`에도 할당되지 않았는지 다시 확인하고, 이미 다른 계정에 귀속됐다면 검증을 실패 처리하고 token을 무효화한다
- 계정 purge 시 남아 있는 verification token 레코드는 `member` 삭제 전에 함께 정리한다

### 13.3 AttendanceRecord

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `member_id`: `BIGINT UNSIGNED` NOT NULL
- `attendance_date`: `DATE` NOT NULL
- `status`: `VARCHAR(32)` NOT NULL
- `memo`: `VARCHAR(255)` NULL
- `created_at`: `DATETIME(6)` NOT NULL
- `updated_at`: `DATETIME(6)` NOT NULL

제약 조건:

- `unique(member_id, attendance_date)`
- `foreign key(member_id) references member(id)`
- 출결 날짜는 DB에 저장되기 전에 애플리케이션에서 반드시 KST 기준으로 계산한다
- 허용 값은 `PRESENT`, `LATE`, `LEAVE_EARLY`, `ABSENT`로 제한하고, 검증은 애플리케이션 계층에서 수행한다

### 13.4 Snippet

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `member_id`: `BIGINT UNSIGNED` NOT NULL
- `title`: `VARCHAR(200)` NOT NULL
- `content_markdown`: `MEDIUMTEXT` NOT NULL
- `created_at`: `DATETIME(6)` NOT NULL
- `updated_at`: `DATETIME(6)` NOT NULL

제약 조건:

- `foreign key(member_id) references member(id)`
- `unique(id, member_id)`

### 13.5 Tag

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `member_id`: `BIGINT UNSIGNED` NOT NULL
- `name`: `VARCHAR(20)` NOT NULL
- `normalized_name`: `VARCHAR(20)` NOT NULL

제약 조건:

- `foreign key(member_id) references member(id)`
- `unique(member_id, normalized_name)`
- `unique(id, member_id)`

규칙:

- `normalized_name`은 소문자화, trim, 연속 공백 정리 등 애플리케이션 정규화를 거친 값으로 저장한다
- 태그는 전역 공용 사전이 아니라 사용자별 어휘로 취급한다
- 같은 문자열 태그라도 사용자마다 독립적으로 가질 수 있다

### 13.6 SnippetTag

- `member_id`: `BIGINT UNSIGNED` NOT NULL
- `snippet_id`: `BIGINT UNSIGNED` NOT NULL
- `tag_id`: `BIGINT UNSIGNED` NOT NULL

제약 조건:

- `primary key(member_id, snippet_id, tag_id)`
- `foreign key(member_id) references member(id)`
- `foreign key(snippet_id, member_id) references snippet(id, member_id) on delete cascade`
- `foreign key(tag_id, member_id) references tag(id, member_id) on delete cascade`

설계 이유:

- 태그를 쉼표 문자열로 저장하지 않고 별도 테이블로 분리해야 검색, 정규화, 확장이 쉬워진다
- 태그를 사용자별로 분리하면 태그 자동완성, 태그 칩, 태그 필터에서 다른 사용자의 태그 어휘가 노출될 위험을 줄일 수 있다
- 스니펫과 연결되는 태그는 반드시 같은 `member_id` 소유 범위 안에서만 생성하고 연결한다
- v1부터 DB 레벨 복합 foreign key로 사용자 소유권 일치를 강제해, 잘못된 코드 한 번으로 교차 사용자 연결이 생기지 않게 한다
- 조인 테이블만 `ON DELETE CASCADE`를 허용하고, 핵심 비즈니스 테이블은 무분별한 연쇄 삭제를 피한다

### 13.7 운영/감사 컬럼 저장 전략

- `attendance_date`는 비즈니스 날짜이므로 `DATE`로 저장한다
- `created_at`, `updated_at`는 기록 생성/수정 시각이므로 `DATETIME(6)` KST로 저장한다
- `TIMESTAMP`는 시간대 자동 변환과 범위 이슈를 피하기 위해 v1에서 사용하지 않는다

이 방식을 택한 이유:

- 출결은 “몇 시에 저장됐는지”보다 “어느 날짜 출결인지”가 더 중요하다
- 실제 사용자, 운영자, 관리자 모두 한국 시간대 기준으로 판단하는 서비스이므로 KST 통일이 해석 오류를 줄인다
- `DATETIME(6)`는 DB 세션 시간대 자동 변환 영향을 덜 받고, 수동 조회 시에도 예측 가능성이 높다
- v1은 대한민국 단일 시간대, 단일 운영자, 단일 리전 전제를 두고 있어 운영 단순성을 우선한다
- 다중 시간대 사용자, 외부 시스템 연동, 로그 상관 분석 요구가 커지면 감사 컬럼은 UTC 저장으로 재검토한다

### 13.8 이 방식의 잠재 리스크

- 애플리케이션 코드 어디선가 `KST`가 아니라 JVM 기본 시간대를 사용하면 날짜 경계 버그가 발생할 수 있다
- 운영 환경 일부가 UTC로 남아 있으면 애플리케이션 데이터와 인프라 로그 시간이 달라져 장애 분석이 헷갈릴 수 있다
- `member.status`나 감사 로그 action처럼 DB `ENUM`을 유지하는 내부 상태는 향후 값 변경 시 마이그레이션 비용이 생길 수 있다
- `DATE`만 저장하면 “실제 몇 시 몇 분에 버튼을 눌렀는지” 자체는 출결 본문 데이터만으로 복원할 수 없다
- 향후 해외 사용자나 다중 시간대 요구가 생기면 현재 KST 고정 설계를 확장해야 한다

대응 방안:

- KST 날짜 변환은 공통 유틸 또는 서비스 계층 한 곳으로 강제한다
- 날짜 경계 테스트를 반드시 작성한다
- 운영 문서에 모든 업무 데이터가 KST 기준이라는 점을 명확히 적는다
- v1에서는 최소 범위의 출결 감사 로그를 남기고, v1.1에서 사용자 열람용 변경 이력 UI 도입 여부를 검토한다

### 13.9 AttendanceAuditLog

- 출결 정보는 장려금 예상치에 직접 영향을 주므로, 생성/수정/삭제 이력을 최소 범위로 남긴다

권장 컬럼:

- `id`: `BIGINT UNSIGNED` PK AUTO_INCREMENT
- `attendance_record_id`: `BIGINT UNSIGNED` NULL
- `member_id`: `BIGINT UNSIGNED` NULL
- `action`: `ENUM('CREATE','UPDATE','DELETE')` NOT NULL
- `before_attendance_date`: `DATE` NULL
- `after_attendance_date`: `DATE` NULL
- `before_status`: `VARCHAR(32)` NULL
- `after_status`: `VARCHAR(32)` NULL
- `changed_by_member_id`: `BIGINT UNSIGNED` NULL
- `changed_at`: `DATETIME(6)` NOT NULL
- `request_ip_hmac`: `CHAR(64)` NULL

운영 원칙:

- 사용자 자신의 변경이라도 모두 기록한다
- 출결 메모 전문은 감사 로그에 복제하지 않아 과도한 개인정보 중복 저장을 피한다
- IP 원문은 저장하지 않고, 정말 필요한 경우에만 서버 비밀키 기반 `HMAC-SHA256` 결과를 `request_ip_hmac`에 저장한다
- `request_ip_hmac`는 기본 보존기간 `30일`을 넘기지 않으며, 키 회전 시 과거 값과의 직접 비교 가능성이 줄어드는 것을 운영 문서에 명시한다
- 삭제 시 원본 `attendance_record`가 사라져도 감사 로그는 보존기간 정책에 따라 별도 유지한다
- 출결 감사 로그의 기본 보존기간은 `30일`로 두고, 계정 purge 시 보존이 필요하면 `attendance_record_id`, `member_id`, `changed_by_member_id`, `request_ip_hmac` 같은 직접 식별 가능한 참조를 `NULL` 또는 동등한 비식별 상태로 정리한 뒤 유지한다
- purge 배치는 감사 로그 비식별화까지 끝난 뒤에만 `member` hard delete를 완료 처리한다
## 14. 출결 상태 허용 값

- `PRESENT`
- `LATE`
- `LEAVE_EARLY`
- `ABSENT`
- DB 저장 타입은 MySQL `ENUM`이 아니라 `VARCHAR(32)`로 두고, 허용 값 검증은 애플리케이션 계층에서 수행한다

## 15. 라우트 / API 설계

### 15.1 페이지 라우트

- `GET /`
- `GET /login`
- `GET /signup`
- `GET /dashboard`
- `GET /attendance`
- `GET /snippets`
- `GET /snippets/{id}`
- `GET /settings`

라우트 원칙:

- 보호 페이지는 모두 현재 로그인 사용자 기준으로 렌더링한다
- v1에서는 `/users/{id}` 형태의 공개 개인 페이지를 두지 않는다
- `GET /snippets/{id}`는 본인 소유 스니펫일 때만 접근 가능하다

### 15.2 폼 / JSON 엔드포인트

- `GET /api/auth/session`
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /auth/login`
- `POST /api/auth/logout`
- `POST /auth/logout`
- `GET /auth/recovery-email/verify?token=...`
- `GET /settings/recovery-email/verify?token=...`
- `GET /api/recovery-email/preview?purpose=...&token=...`
- `POST /api/recovery-email/confirm`
- `GET /api/attendance?yearMonth=2026-03`
- `PUT /api/attendance/{attendanceDate}`
- `DELETE /api/attendance/{attendanceRecordId}`
- `GET /api/snippets`
- `GET /api/snippets/{id}`
- `POST /api/snippets`
- `PUT /api/snippets/{id}`
- `DELETE /api/snippets/{id}`
- `PATCH /api/settings/profile`
- `POST /api/settings/password`
- `POST /api/settings/recovery-email`
- `GET /settings/recovery-email/verify?token=...`
- `POST /api/settings/recovery-email/resend`
- `POST /api/settings/account-deletion`

엔드포인트 원칙:

- 일반 사용자용 엔드포인트는 요청 바디나 쿼리스트링으로 임의의 `memberId`를 받지 않는다
- 사용자 구분은 항상 인증 세션에서 결정한다
- 브라우저 화면은 React `/app`에서 렌더링하고, 데이터 변경은 same-origin JSON API와 적절한 HTTP 메서드(`GET/POST/PATCH/PUT/DELETE`)로 처리한다
- 옛 `/login`, `/dashboard`, `/attendance`, `/snippets`, `/settings` 경로는 사용자 호환용 리다이렉트만 유지하고, 새 화면 진입점은 `/app/...`으로 통일한다
- 스니펫 저장 시 시크릿 패턴이 감지되고 유효한 `secretWarningToken`이 없으면 저장하지 않고 경고 응답을 반환한다
- `secretWarningToken`은 서버가 경고 응답 때 발급하고, 현재 로그인 사용자 + 현재 세션 + 현재 content fingerprint + `10분` TTL에 묶어 검증한다
- 이미 사용했거나 만료됐거나 fingerprint가 달라진 token은 인정하지 않는다
- `recovery_email` 검증 링크의 `GET` 요청은 토큰을 소비하지 않고 `/app/verify-email?purpose=...&token=...` 확인 화면으로 연결하며, 실제 토큰 소비와 이메일 검증 완료 처리는 `GET /api/recovery-email/preview`와 `POST /api/recovery-email/confirm`에서 처리한다
- `recovery_email` 검증 token은 `30분` TTL의 1회용으로 발급하고, 현재 대상 계정과 신규 이메일 값에 묶어 검증한다
- 이미 검증 완료된 token에 대한 재방문 `GET`은 안전한 완료 화면으로 처리하고, 메일 스캐너 선조회나 사용자의 단순 링크 열기만으로는 검증 완료가 되지 않게 한다
- `POST /api/auth/signup`에는 보안 요구사항에서 정의한 최소 `IP당 5회/1시간` 제한과 verification 메일 발송 남용 방지 기준을 적용한다
- 새 recovery email verification token을 발급할 때는 같은 계정과 같은 목적의 기존 미사용 token을 먼저 무효화한다
- `POST /api/settings/recovery-email/resend`는 인증된 현재 로그인 사용자만 호출할 수 있으며, 현재 계정의 가장 최근 미소비 verification flow target에 대해서만 재발송한다
- 회원가입 직후 미검증 상태에서의 재발송도 별도 `/auth/.../resend` 엔드포인트를 두지 않고 `POST /api/settings/recovery-email/resend`로 통일한다
- 재발송 가능한 target이 없거나 이미 검증이 완료된 경우에는 안전한 no-op 또는 안내 응답으로 처리하고, 다른 계정 대상 재발송은 허용하지 않는다
- `POST /api/settings/recovery-email/resend`는 계정/IP 단위 속도 제한과 재발송 쿨다운을 적용한다
- v1 현재 구현의 스니펫 검색/태그 필터는 `GET /api/snippets` 전체 목록을 불러온 뒤 현재 로그인 사용자 범위 안에서 클라이언트 필터링으로 처리한다
- v1의 스니펫 목록은 `updatedAt desc` 고정 정렬을 사용하고, 다중 태그 조합/사용자 선택 정렬/페이지네이션은 지원하지 않는다
- 시크릿 경고가 발생하면 프론트는 현재 입력값을 유지한 채 경고 상태를 보여주고, API는 `409`와 경고 DTO를 반환한다

### 15.3 대표 요청 / 응답 DTO 초안

`SignupRequest`

- `username`
- `password`
- `displayName`
- `recoveryEmail`

`RecoveryEmailChangeRequest`

- `newRecoveryEmail`
- `currentPassword`

`RecoveryEmailVerificationPreviewResponse`

- `purpose`
- `maskedTargetEmail`
- `verificationExpiresAt`
- `alreadyConsumed`
- `invalid`

`RecoveryEmailVerifyConfirmRequest`

- `token`

`RecoveryEmailVerificationResultResponse`

- `verified`
- `maskedRecoveryEmail`

`AttendanceUpsertRequest`

- `attendanceDate`
- `status`
- `memo`

`MonthlySummaryResponse`

- `yearMonth`
- `presentCount`
- `lateCount`
- `leaveEarlyCount`
- `absentCount`
- `convertedAbsenceCount`
- `deductionAmount`
- `expectedAllowanceAmount`

`CalendarDayResponse`

- `date`
- `status`
- `isToday`

`SnippetFormRequest`

- `title`
- `contentMarkdown`
- `tags`
- `secretWarningToken`

`SnippetSearchRequest`

- `q`
- `tag`

`SnippetSecretWarningResponse`

- `warningCodes`
- `message`
- `requiresAcknowledgement`
- `secretWarningToken`
- `contentFingerprint`
- `expiresAt`

`SnippetSearchItemResponse`

- `id`
- `title`
- `tags`
- `updatedAt`
- `highlightText`

DTO 원칙:

- 페이지 렌더링용 모델과 API 응답 DTO를 구분한다
- 엔티티를 그대로 JSON 응답이나 템플릿 바인딩 모델로 노출하지 않는다
- 날짜 응답 형식은 KST 기준 `yyyy-MM-dd` 또는 `yyyy-MM` 문자열로 통일한다

### 15.4 대시보드 뷰모델 초안

`DashboardPageData`

- `displayName`
- `todayDate`
- `todayAttendanceStatus`
- `monthlySummary`
- `recentSnippets`
- `emptyAttendance`
- `emptySnippets`

구성 원칙:

- 대시보드에 필요한 데이터는 세션 응답, 출결 월 요약 응답, 스니펫 목록 응답을 조합해 프론트 store 또는 화면 로더에서 구성한다
- UI 내부에서는 별도 DB 접근이나 복잡한 계산을 하지 않고, 서버 `monthlySummary`와 정렬된 스니펫 데이터를 그대로 사용한다

## 16. 프론트엔드 상세 요구사항

### 16.1 디자인 방향

- 기본은 라이트 테마
- 차분하지만 서비스다운 톤
- 카드 구조는 쓰되 과밀하지 않게 설계
- “학원 훈련생이 실제로 쓰는 생활형 제품” 느낌을 유지

### 16.2 모바일 요구사항

- 엄지 영역을 고려한 하단 여백 확보
- 출결 입력은 고정형 빠른 액션 버튼 제공 가능
- 폼 요소는 모바일 탭 영역 기준으로 충분히 크게 구성
- 중요 정보는 과도한 스크롤 없이 첫 화면에 보이도록 한다

### 16.3 데스크톱 요구사항

- 학원 PC나 개인 노트북에서 장시간 사용해도 피로하지 않은 레이아웃
- 대시보드, 캘린더, 스니펫 목록에서 정보 탐색 속도가 빨라야 한다
- 마우스 사용 기준의 hover, focus, 클릭 상태를 명확히 제공한다
- 넓은 화면에서 카드, 표, 필터, 검색 영역이 유기적으로 배치되어야 한다

### 16.4 성능 요구사항

- 무거운 SPA 번들 지양
- 클라이언트 JS 최소화
- React `/app` 프론트는 route lazy loading과 코드 분할을 사용해 번들 증가를 억제한다

## 17. 검색 요구사항

- 스니펫 제목과 내용 기준 키워드 검색
- 태그 정확 일치 필터
- v1 현재 구현은 `GET /api/snippets` 목록 응답을 받은 뒤 제목/내용/태그 기준으로 클라이언트 필터링한다
- `q`와 `tag`는 모두 선택이며, 둘을 함께 사용할 수 있다
- `tag`는 현재 로그인 사용자 소유의 단일 태그 정확 일치만 지원한다
- 결과 정렬은 `updatedAt desc` 고정이다
- v1에서는 페이지네이션, 다중 태그 조합, 사용자 지정 정렬을 지원하지 않는다
- v1 검색 응답은 최대 `50건`까지만 반환한다
- v1에서는 사용자별 데이터 규모가 작다는 전제에서 제목/태그 우선 검색 + 본문 부분 일치 검색으로 시작한다
- `content_markdown` 본문 검색은 초기에는 `%LIKE%` 기반 테이블 스캔을 허용하되, 사용자별 데이터가 적다는 전제를 문서화한다
- v1의 `%LIKE%` 기반 검색 전제는 사용자당 대략 `500개 이하`의 스니펫 또는 누적 `content_markdown` 약 `10MB 이하` 규모를 가정한다
- 위 가드레일을 넘기면 페이지네이션, stronger search, 또는 저장 구조 조정을 v1.1보다 앞당겨 검토한다
- MySQL 기본 `FULLTEXT`는 한글 검색 품질이 제한될 수 있으므로 v1.1 이후 `ngram parser` 또는 외부 검색 도입 여부를 다시 판단한다
- 태그 칩, 태그 자동완성, 태그 필터 후보는 항상 현재 로그인 사용자 소유의 태그만 대상으로 생성한다

## 18. 보안 요구사항

- Spring Security 세션 인증
- 폼 요청에 대해 CSRF 활성화
- 운영 환경에서 Secure Cookie 사용
- 로그인 흐름에 맞춰 SameSite 설정
- `POST /auth/login`에는 최소 `IP당 10회/10분`, `username당 실패 5회/15분` 기준의 제한 또는 동등한 완화책을 둔다
- `POST /api/auth/signup`에는 최소 `IP당 5회/1시간` 기준의 제한을 둔다
- v1.1에서 CAPTCHA나 더 강한 비정상 로그인 방어를 검토한다
- 시크릿은 환경변수로 관리하고 저장소에 넣지 않는다
- 로그인 실패, 회원가입 중복, 비밀번호 재설정 관련 오류 메시지는 계정 존재 여부를 과도하게 노출하지 않는 방향으로 설계한다
- Nginx와 애플리케이션 중 최소 한 곳에서 로그인 속도 제한을 적용하고, 가능하면 IP 기준 1차 제한 + 계정 기준 2차 완화책을 함께 둔다
- 운영 환경에서는 `Content-Security-Policy`, `X-Frame-Options` 또는 `frame-ancestors`, `X-Content-Type-Options`, `Referrer-Policy` 같은 기본 보안 헤더를 적용한다
- 세션 유휴 만료 시간은 `30분`, 절대 만료 시간은 `12시간`으로 고정하고 장시간 미사용 세션은 자동 만료되게 한다
- `GET /actuator/health` 외의 Actuator 엔드포인트는 기본적으로 비활성화하거나 인증/내부 접근으로 제한한다
- 운영 로그에는 비밀번호, 세션 식별자, 쿠키 값, DB 연결 문자열, S3 민감 경로, 전체 요청 본문을 남기지 않는다
- 스니펫 본문에는 개인 토큰, API 키, 비밀번호, 내부 계정 정보가 들어갈 수 있으므로 운영 로그와 에러 리포트에 전체 본문을 그대로 남기지 않는다
- `recovery_email`은 개인정보로 취급하고, 화면/로그/에러 응답에는 필요 최소 범위만 마스킹해 노출한다
- `recovery_email` 검증 링크 또는 token은 `30분` TTL, 1회 사용, 사용자/대상 이메일 바인딩을 만족해야 한다
- 이메일 검증 링크는 `GET`으로 바로 계정 상태를 바꾸지 않고, 미리보기/확인 화면만 제공한 뒤 사용자의 명시적 `POST`로만 토큰을 소비한다
- `POST /api/settings/recovery-email`과 `POST /api/settings/recovery-email/resend`에는 최소 `계정당 5회/1시간`, `IP당 10회/1시간` 기준의 제한을 둔다
- `POST /api/settings/recovery-email/resend`는 같은 계정 기준 `1분` 쿨다운을 둔다
- 미검증 recovery email target은 pending 상태로만 보관하고, 검증 성공 전에는 전역 unique address를 점유하지 않게 설계한다
- 운영자 수동 비밀번호 초기화는 본인 확인 절차, 수행자 기록, 수행 시각, 대상 계정 기록을 반드시 남겨야 한다
- 원격 비밀번호 초기화와 원격 삭제 요청은 검증된 `recovery_email` 확인을 통과한 경우에만 처리한다
- 운영 DB 덤프나 백업 파일을 개발자 개인 노트북이나 일반 개발 환경에 그대로 반입하지 않는다
- 테스트나 디버깅 목적으로 운영 데이터를 써야 한다면 허용 목록 기반으로 재구성한 별도 사본만 사용한다
- 허용 목록에 없는 필드(`password_hash`, `recovery_email`, 출결 `memo`, `request_ip_hmac`, 세션/쿠키/토큰 값, 원문 스니펫 본문)는 기본적으로 제거하고, 필요한 경우에도 비식별 샘플로 대체한다
- 운영자용 SSH, DB, AWS 접근 권한은 공용 계정을 공유하지 않고 개인별 식별 가능한 계정 또는 역할 기반으로 관리한다
- 회원 탈퇴 처리, 비밀번호 초기화, 백업 복원 같은 고위험 운영 행위는 수행자, 사유, 대상, 시각을 남긴다
- 스니펫 저장 화면의 시크릿 경고는 단순 UI 문구에 그치지 않고, 저장 API에서도 동일한 탐지 규칙을 재사용해 서버 로그 누락이나 우회를 줄인다
- 시크릿 경고 token은 재사용 불가 `10분` TTL 토큰으로 발급하고, 세션/사용자/내용 fingerprint가 모두 일치할 때만 1회 인정한다
- `request_ip_hmac`가 필요한 경우 서버 비밀키 기반 `HMAC-SHA256`만 허용하고, 일반 해시나 단순 마스킹을 장기 저장 기준으로 사용하지 않는다
- `request_ip_hmac` 보존기간과 HMAC 키 회전 정책을 운영 문서에 명시한다
- 현재 v1의 프록시 체인은 `client -> nginx -> app`으로 가정한다
- Nginx는 클라이언트가 보낸 `X-Forwarded-For`, `X-Real-IP`를 그대로 신뢰하지 않고, 프록시 구간에서 재설정한 값만 애플리케이션에 전달한다
- 애플리케이션은 신뢰된 리버스 프록시(Nginx)에서 전달된 forwarded header만 사용하고, 인터넷에서 직접 들어온 spoofed header는 신뢰하지 않는다

## 19. AWS 배포 명세

### 19.1 v1 운영 토폴로지

- EC2 `t3.small` 인스턴스 1대
- 공인 IPv4 1개
- Docker Compose
- 컨테이너 구성:
  - nginx
  - bootsync-app
  - mysql
- DB 백업 전용 S3 버킷 1개
- 복구용 이메일 발송을 위한 SES 또는 동등한 트랜잭션 이메일 서비스 1개

운영 메모:

- v1 첫 배포의 기본 인스턴스는 `t3.small`로 정한다
- 첫 배포 안정화와 운영 절차 검증이 끝난 뒤 비용 최적화가 필요하면 `t4g.small` 전환을 검토한다
- `t4g`는 ARM64 환경이므로 애플리케이션과 모든 컨테이너 이미지가 ARM64 또는 멀티 아키텍처를 지원해야 한다
- 로컬 개발 환경이 Windows x86_64라면 `t4g` 전환 전에 Docker Buildx 멀티 아키텍처 빌드 또는 JAR 중심 배포 절차를 명시적으로 마련한다
- 보안 그룹은 `80/443`만 외부에 공개하고, MySQL 포트는 외부에 공개하지 않는다
- EC2 EBS 볼륨 암호화를 활성화한다
- 현재 v1에서 Nginx의 IP 기준 rate limit은 직접 접속한 클라이언트 IP를 기준으로 동작한다
- 애플리케이션에서 클라이언트 IP가 필요할 경우 Nginx가 재설정한 `X-Real-IP`, `X-Forwarded-For`만 신뢰하고, 신뢰 프록시 범위를 운영 문서에 명시한다

### 19.2 도메인 / HTTPS

- Route 53 hosted zone 사용
- Nginx 리버스 프록시 사용
- Let’s Encrypt + Certbot으로 HTTPS 적용
- recovery email 발송에 사용하는 발신 이메일 또는 발신 도메인은 SES에서 검증된 identity만 사용한다
- SES를 사용할 경우 샌드박스 여부를 배포 전에 확인하고, 공개 출시 전에는 production access 확보 또는 동등한 대체 메일 서비스를 준비한다

설계 이유:

- ACM은 무료지만 EC2 단독 구성에서는 ALB나 CloudFront 없이 바로 쓰기 불편하다
- v1에서 ALB는 월 고정비가 커서 제외한다

### 19.3 백업 전략

- 하루 1회 MySQL dump 생성
- S3에 업로드
- 최근 7일 일일 백업 + 최근 4주 주간 백업 유지
- 백업 생성 성공 여부를 작업 로그에 남긴다
- 백업 실패는 다음 백업 시점 전까지 운영자가 감지할 수 있도록 로그 + 알람 또는 동등한 확인 절차를 둔다
- 월 1회 이상 복원 리허설 또는 덤프 파일 무결성 검증을 수행한다
- 백업 버킷은 운영 애플리케이션의 일반 정적 파일/첨부 업로드와 분리한다
- 향후 첨부 파일 저장이 필요해져도 백업과 같은 버킷을 재사용하지 않고 별도 버킷 또는 최소한 별도 prefix, lifecycle, IAM 정책을 둔다
- 백업 버킷은 `Block Public Access`를 활성화하고, 기본 암호화(SSE-S3 또는 SSE-KMS)를 적용한다
- 백업 업로드 권한과 복원 읽기 권한은 가능하면 IAM 차원에서 분리한다
- 백업 파일에는 사용자 데이터와 비밀번호 해시가 포함될 수 있으므로 민감정보로 취급한다
- 삭제 요청 계정 목록은 앱 외부의 접근 통제된 `삭제 요청 등록부`로 별도 관리하고, 최소 마지막 관련 백업 만료 시점까지 보존하며 백업 복원 전 해당 목록을 기준으로 수동 scrub 한다

### 19.4 최소 운영 안정성 기준

- `GET /actuator/health` 또는 동등한 헬스체크 엔드포인트를 둔다
- Docker Compose의 각 서비스에 재시작 정책을 명시한다
- EC2 상태 검사 실패 알람과 디스크 사용량 알람은 v1부터 설정하는 것을 권장한다
- 장애 시 확인 순서는 `nginx -> app -> mysql -> 백업 최근 성공 여부`로 문서화한다
- Certbot 자동 갱신 스케줄과 갱신 후 Nginx reload 절차를 운영 문서에 명시한다
- 운영 복구 목표는 `RPO 24시간`, `RTO 8시간`을 기준으로 runbook과 점검표를 만든다
- 운영자 수동 비밀번호 초기화/삭제 요청 runbook에는 최소 `접수 채널`, `본인 확인 단계`, `기록 저장 위치`, `승인/수행 주체`, `실행 후 통지 절차`를 포함한다
- 원격 고위험 요청의 접수 채널은 검증된 recovery email 회신이 가능한 운영 메일함 또는 접근 통제된 운영 티켓으로 제한한다
- 계정 삭제 purge 배치의 실행 주기, 실패 감지, 재시도 절차를 운영 문서에 명시한다
- purge 배치는 일부 계정이 이미 부분 삭제된 상태에서도 재실행 가능한 멱등 작업으로 구현하거나 동등한 수동 절차를 마련한다
- 복구 runbook에는 `가장 최근 성공 백업 확인 -> 복구 대상 DB 준비 -> 덤프 복원 -> 삭제 요청 등록부 기준 삭제 계정 수동 scrub -> 앱 재기동 -> 로그인/대시보드/출결 저장 스모크 테스트 -> 서비스 재오픈 -> 사고 기록` 순서를 포함한다
- 삭제 계정 scrub이 끝나기 전에는 공개 트래픽을 열지 않는다
- 배포 직후와 복구 직후에는 `로그인`, `대시보드 조회`, `출결 저장`, `스니펫 조회` 4가지 핵심 흐름을 최소 스모크 테스트로 확인한다

## 20. AWS 서비스 선택

### 20.1 v1 사용 서비스

- EC2
- EBS
- S3
- SES
- Route 53
- 필요 시 최소 범위의 CloudWatch 알람

### 20.2 v1.1 선택 가능 서비스

- CloudWatch 로그 수집 고도화 및 대시보드
- SES 발송 품질 모니터링/템플릿 고도화
- 사용자가 늘어난 뒤 RDS 전환

### 20.3 v1 비권장 서비스

- ECS
- EKS
- ElastiCache
- ALB
- CloudFront

## 21. 비용 중심 배포 판단

### 21.1 첫 운영 환경 권장안

- 비공개 테스트 또는 초기 검증 단계: `t3.micro`
- 초기 공개 운영 단계: `t3.small`
- 30GB gp3 EBS
- 공인 IPv4 1개
- S3 백업 10GB 이하
- Route 53 hosted zone 1개

이 구성이 가장 적절한 이유:

- 현재 로컬 x86_64 개발 환경과 운영 환경의 아키텍처를 맞춰 첫 배포 리스크를 낮출 수 있다
- 비용이 여전히 낮은 편이다
- 소규모 실사용 서비스에 충분한 성능이다
- 복구 구조가 단순하다
- AWS 포트폴리오 가치도 충분하다
- 첫 배포 안정화 후 ARM64 빌드 경로가 검증되면 `t4g.small` 전환으로 비용 최적화를 다시 검토할 수 있다

### 21.2 업그레이드 기준

- 백업/복구 부담이 커질 때 RDS 전환
- 실제 사용자가 늘어 앱과 DB 분리가 필요할 때 구조 확장
- 세션/캐시 병목이 생길 때만 Redis 검토

### 21.3 첫 운영 월비용 추정

추정 기준:

- 리전: 서울 (`ap-northeast-2`)
- 월 사용시간: 약 730시간
- 앱과 DB를 동일 EC2에 같이 운영
- 공개 서비스이지만 초기 소규모 사용자 기준
- 비용 추정은 `2026-03` 기준 참고치이며, 실제 배포 직전에는 AWS 공식 요금표와 VAT/환율 조건을 다시 확인한다

권장 저비용 운영안:

- EC2 `t3.small`: 약 `$18.98/month`
- EBS gp3 `30GB`: 약 `$2.74/month`
- 공인 IPv4 `1개`: 약 `$3.65/month`
- S3 `10GB`: 약 `$0.25/month`
- Route 53 hosted zone `1개`: `$0.50/month`

예상 합계:

- 세전 약 `$26.12/month`

한국 계정 기준 VAT 10%가 붙는 경우 대략:

- 약 `$28.73/month`
- 별도 도메인 등록비는 제외
- 저용량 recovery email 발송용 SES 비용은 사용량이 매우 작다는 전제에서 위 합계에서 제외했다
- AWS 프로모션 크레딧이 있는 경우 위 사용료는 먼저 크레딧에서 차감되며, 도메인 등록비 등 일부 항목은 별도 결제될 수 있다
- 같은 구성에서 `t4g.small`로 전환하면 EC2 비용은 약 `$15.18/month`, 전체 합계는 세전 약 `$22.32/month` 수준으로 내려간다
- 즉 `t3.small` 첫 배포는 월 약 `$3.80`의 추가 비용으로 아키텍처 리스크를 낮추는 선택이다

### 21.4 RDS를 초반부터 붙일 경우

예시:

- EC2 `t3.small`: 약 `$18.98/month`
- EC2 EBS gp3 `20GB`: 약 `$1.82/month`
- 공인 IPv4 `1개`: 약 `$3.65/month`
- RDS MySQL `db.t4g.micro` Single-AZ: 약 `$18.25/month`
- RDS GP3 스토리지 `20GB`: 약 `$2.62/month`
- S3 `10GB`: 약 `$0.25/month`
- Route 53 hosted zone `1개`: `$0.50/month`

예상 합계:

- 세전 약 `$46.07/month`

판단:

- v1에서는 RDS를 바로 쓰지 않는 편이 맞다
- 실사용자가 붙고 운영 리스크가 실제 문제로 드러난 뒤 전환하는 것이 적절하다
- RDS는 관리형 서비스라 EC2 앱 서버를 `t3.small`로 시작하더라도 동일한 ARM64 빌드 리스크를 만들지는 않는다

### 21.5 피해야 할 비용 함정

- v1에서 ALB를 붙이지 않는다
- v1에서 NAT Gateway를 만들지 않는다
- 정적 자산 트래픽이 의미 있게 커지기 전까지 CloudFront를 붙이지 않는다
- 로그를 과하게 쌓고 보존기간을 무제한으로 두지 않는다
- 사용하지 않는 공인 IPv4를 붙여두지 않는다

## 22. 구현 단계

해석 원칙:

- Phase 1~3은 `로컬 개발 완료`와 `비공개 테스트 가능` 상태를 만드는 제품 핵심 구현 구간이다
- Phase 4~5는 `공개 출시 가능` 상태를 만드는 운영/보안/복구 준비 구간이다

### Phase 1

- 프로젝트 초기 세팅
- Flyway 설정
- Member 및 인증 구현
- recovery email 검증 흐름 구현
- 세션 상태 재검사와 삭제 계정 강제 로그아웃 구현
- 공통 레이아웃 구축

### Phase 2

- 대시보드 구현
- 출결 CRUD 구현
- 월별 장려금 요약 구현
- 출결 감사 로그 구현

### Phase 3

- 스니펫 CRUD 구현
- 검색 및 태그 기능 구현
- Markdown 렌더링 적용

### Phase 4

- 반응형 UI 다듬기
- 에러 처리
- 빈 상태 화면 정리
- 개인정보/탈퇴 운영 문서 정리
- 삭제 요청 등록부와 삭제 계정 scrub 운영 절차 정리
- 계정 삭제 purge 배치 구현 및 운영 절차 정리
- 운영 배포

### Phase 5

- 백업 자동화
- 기본 모니터링
- 복구 runbook 작성 및 복원 리허설
- 실제 훈련생 3~5명 사용자 테스트

## 23. 수용 기준

- 아래 수용 기준은 `공개 출시 가능` 상태를 기준으로 하며, `로컬 개발 완료` 기준은 `26.1`, 단계 구분은 `4.3`을 따른다

- 사용자가 모바일에서 회원가입과 로그인을 할 수 있어야 한다
- 회원가입 시 `recovery_email`을 입력하고 인증 메일을 받을 수 있어야 한다
- 회원가입 직후 미검증 상태에서는 `member.recovery_email`이 비어 있거나 검증 완료 전용 상태여야 하며, 입력만으로 전역 unique recovery email을 선점하면 안 된다
- 인증된 `recovery_email`만 원격 비밀번호 초기화와 원격 삭제 요청 본인 확인 수단으로 인정되어야 한다
- recovery email 재발송은 로그인한 현재 사용자 기준 `POST /api/settings/recovery-email/resend` 하나로 처리되고, 다른 계정 대상 재발송이 가능하면 안 된다
- 사용자 A와 사용자 B가 각각 로그인했을 때 서로 다른 개인 대시보드와 개인 데이터가 보여야 한다
- 사용자 A가 사용자 B의 출결 또는 스니펫 URL을 직접 요청해도 접근할 수 없어야 한다
- 사용자가 날짜별로 하루 1회 출결을 입력할 수 있어야 한다
- 월별 예상 장려금 계산이 규칙에 맞아야 한다
- `지각 2회 + 조퇴 1회`가 자동으로 결석 1회로 합산되지 않아야 한다
- 저장하지 않은 날짜가 자동 결석으로 잡히지 않아야 한다
- 미래 날짜 출결 입력이 차단되어야 한다
- 동일 날짜 출결을 브라우저 이중 제출해도 서버 `500` 없이 처리되어야 한다
- 출결 생성/수정/삭제 시 최소 감사 로그가 남아야 한다
- 사용자가 스니펫을 저장하고 키워드/태그로 다시 찾을 수 있어야 한다
- 스니펫 저장 시 시크릿 패턴이 감지되면 첫 저장 요청은 저장되지 않고 사용자 경고와 `secretWarningToken`이 표시되어야 하며, 같은 내용과 같은 세션으로 재요청했을 때만 저장되어야 한다
- 서비스가 AWS에서 HTTPS로 동작해야 한다
- 백업이 매일 생성되어야 한다
- 보호된 쓰기 요청은 CSRF 토큰 없이 성공하면 안 된다
- 운영 환경 쿠키는 `HttpOnly`, `Secure`, `SameSite=Lax` 조건을 만족해야 한다
- 외부에서 노출되는 Actuator 엔드포인트는 `health` 수준으로 제한되어야 한다
- 태그 자동완성/필터 후보에 다른 사용자 태그가 노출되면 안 된다
- snippet-tag 연결은 DB 레벨에서 다른 사용자 tag/snippet 조합을 거부해야 한다
- 백업 복원 리허설이 실제로 성공하고, 문서화한 `RTO 8시간` 목표 안에서 수행 가능해야 한다
- 삭제 요청 처리된 계정은 기존 로그인 세션을 가지고 있어도 다음 보호 페이지 요청부터 즉시 차단되어야 한다
- `delete_due_at` 이전 운영자 취소가 수행된 계정은 `ACTIVE`로 복구될 수 있어야 하고, 취소 전 세션은 재사용되지 않아야 한다
- 백업 복원 후 삭제 요청 등록부 기준 수동 scrub을 거친 상태에서 삭제 요청 계정이 다시 로그인 가능 상태로 되살아나면 안 된다
- 테스트/디버깅용 운영 데이터 사본에는 허용 목록 밖의 민감 필드가 포함되면 안 된다
- 운영자 탈퇴/삭제 처리 절차와 개인정보 처리 고지가 공개 출시 전에 준비되어 있어야 한다
- 실제 사용자 몇 명이 개발자 도움 없이 사용 가능해야 한다

## 24. 출시 후 확인 지표

- 7일 활성 사용자 수
- 사용자당 주간 출결 입력 횟수
- 사용자당 주간 스니펫 작성 횟수
- 재로그인 비율
- 검색 기능 사용 비율
- recovery email 인증 완료율
- 백업 성공률
- 최근 복원 리허설 이후 경과 일수
- 출결 중복 제출 또는 저장 충돌 발생률

## 25. 향후 확장

- 스니펫 고정(pin) 기능
- 스니펫 공유 기능
- 월별 출결 리포트 내보내기
- 스니펫 이미지 첨부
- 이메일 리마인더
- 학원 관리자 지원 기능
- 다중 학원/다중 반 지원 구조

## 26. 구현 상세 가이드

### 26.1 프로젝트 스캐폴딩 완료 기준

- 현재 저장소는 명세 문서 중심 상태이며, 아래 산출물이 생성되어야 비로소 구현 프로젝트로 본다
- `settings.gradle`
- `build.gradle`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- `src/main/java/com/bootsync/**`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/templates/**`
- `src/main/resources/static/**`
- `src/main/resources/db/migration/V1__baseline.sql`
- `src/test/java/com/bootsync/**`
- `src/test/resources/**`
- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`
- `.gitignore`
- `README.md`

완료 기준:

- `./gradlew test` 또는 동등한 명령이 로컬에서 실행 가능해야 한다
- 로컬 MySQL 컨테이너와 애플리케이션이 함께 기동되어 로그인 화면까지 확인 가능해야 한다
- 최초 Flyway 마이그레이션으로 스키마가 생성되어야 한다

### 26.2 권장 패키지 / 모듈 구조

- `com.bootsync.common`: 공통 예외, 시간 처리, 웹 공통 응답, 유틸, 설정
- `com.bootsync.config`: Security, MVC, JPA, Flyway, S3, SES, Actuator 관련 설정
- `com.bootsync.auth`: 회원가입, 로그인, 세션 사용자 객체, 인증 로직
- `com.bootsync.member`: 회원 엔티티, 프로필 수정, 비밀번호 변경, recovery email 검증, 계정 상태 전환, 삭제 요청 처리 관리
- `com.bootsync.dashboard`: 대시보드 조회 전용 서비스와 뷰모델
- `com.bootsync.attendance`: 출결 엔티티, 서비스, 요약 계산, 캘린더 조회, 감사 로그
- `com.bootsync.snippet`: 스니펫 엔티티, 검색, 태그 조합, Markdown 렌더링
- `com.bootsync.tag`: 태그 정규화와 태그 조회 책임
- `com.bootsync.settings`: 사용자 설정 화면 관련 기능

구조 원칙:

- 도메인별로 `controller`, `service`, `repository`, `dto`, `entity`를 가까이 둔다
- 공통 코드는 `common`으로 올리되, 도메인 규칙까지 공통화하지 않는다
- 화면 렌더링에 JPA 엔티티를 그대로 넘기기보다 뷰모델 또는 응답 DTO를 사용한다

### 26.3 컨트롤러 / 서비스 / 리포지토리 책임 분리

- 컨트롤러는 요청 파싱, 인증 사용자 확인, 응답/뷰 선택까지만 담당한다
- 비즈니스 규칙은 서비스 계층에서 처리한다
- 리포지토리는 조회와 저장만 담당하고, 화면 규칙이나 계산 규칙을 포함하지 않는다
- 장려금 계산은 `AttendanceService` 내부 private 메서드보다 별도 `AllowanceCalculator` 성격의 컴포넌트로 분리하는 편이 테스트에 유리하다
- 대시보드 조합 조회는 `DashboardService`가 담당하고, 여러 도메인 데이터를 한 번에 모아 뷰모델로 반환한다

### 26.4 DTO 및 입력 검증 규칙

회원가입/회원정보:

- `username`: `4~20자`, 영문 소문자, 숫자, `_`만 허용
- `password`: `10~64자`, 공백 금지, 너무 약한 패턴과 흔한 비밀번호는 거절
- `display_name`: `2~20자`, 앞뒤 공백 제거 후 저장
- `recovery_email`: 필수, 일반 이메일 형식, 소문자 trim 정규화 후 저장, 검증 완료 주소 기준 중복 불가
- 회원가입 요청의 `recovery_email`은 검증 target 입력값으로 필수지만, 검증 성공 전에는 `member.recovery_email`에 반영하지 않는다
- 복구용 이메일 변경은 현재 비밀번호 확인과 새 이메일 검증 완료 후에만 반영한다
- 새 recovery email 적용 직전에도 중복 여부를 다시 확인해 최종 반영 시점 정합성을 지킨다
- `POST /api/settings/recovery-email/resend`는 현재 로그인 계정에 대해서만 동작하며, 같은 계정 기준 `1분` 쿨다운을 둔다

출결:

- `attendance_date`: 필수, KST 기준 `LocalDate`
- v1에서는 미래 날짜 등록을 허용하지 않는다
- `status`: 허용 값 집합(`PRESENT`, `LATE`, `LEAVE_EARLY`, `ABSENT`)만 허용
- `memo`: 선택, `255자` 이하
- `memo`는 월 요약과 장려금 계산에 영향을 주지 않는다

스니펫:

- `title`: 필수, `1~200자`
- `content_markdown`: 필수, 빈 문자열 불가
- `content_markdown` 최대 길이는 `20,000자`로 제한한다
- `content_markdown` 저장 전 시크릿 패턴 감지 결과를 함께 계산해 경고 여부를 결정한다
- `secretWarningToken`: 선택, 시크릿 경고 응답에서 서버가 발급한 토큰일 때만 인정한다
- `tags`: 중복 제거 후 저장, 태그 수는 예를 들어 `10개 이하`, 각 태그 길이는 `20자 이하`로 제한한다
- 검색어 `q`는 공백 정리 후 최대 길이 `100자`로 제한한다
- 검색용 `tag`는 선택 단일 값이며, 정규화한 뒤 현재 로그인 사용자 소유 태그와 정확 일치 비교한다

검증 원칙:

- 폼 DTO에 Bean Validation을 적용한다
- 서버 검증 실패 시 사용자 입력값을 최대한 유지한 채 다시 렌더링한다
- 동일 날짜 중복 출결은 검증과 DB unique 제약으로 둘 다 막는다
- `username`, `display_name`, `tag` 길이 제한은 애플리케이션 검증과 DB 컬럼 길이를 동일하게 유지한다
- recovery email verification token 원문은 DB나 로그에 남기지 않고, 서버 저장 모델에는 hash만 저장한다
- recovery email verification token TTL 기본값은 `30분`으로 고정한다
- 시크릿 경고는 단순 프론트 상태가 아니라 서버 비즈니스 규칙으로 판단하고, 1차 요청은 경고 응답 후 저장하지 않는다
- 서버는 `content_markdown`의 정규화 fingerprint를 계산하고, 경고 응답에 포함된 fingerprint와 현재 요청이 일치할 때만 `secretWarningToken`을 인정한다
- `secretWarningToken`은 현재 세션/사용자에 귀속되고 `10분` TTL과 1회 사용 제한을 가진다

### 26.5 인증 / 세션 구현 기준

- Spring Security의 `formLogin` 기반 구성을 사용한다
- 인증 principal에는 최소한 `memberId`, `username`, `displayName`을 담는다
- 로그인 시 `member.status == ACTIVE`를 확인하고, `PENDING_DELETE`/`DISABLED` 계정은 공통 오류 메시지로 차단한다
- 이미 로그인한 사용자라도 보호된 요청을 처리하기 전에 현재 `member.status`를 다시 확인하고, `ACTIVE`가 아니면 세션을 즉시 무효화한 뒤 로그아웃 처리한다
- 회원가입 직후 `recovery_email` 검증 메일을 발송하고, 검증 완료 전까지는 원격 복구 수단으로 인정하지 않는다
- 회원가입 검증과 recovery email 변경 검증은 `RecoveryEmailVerificationToken` 저장 모델을 공통 사용하고, `purpose`로 구분한다
- 원격 비밀번호 초기화와 원격 삭제 요청은 `recovery_email_verified_at`가 있는 계정에 대해서만 허용한다
- `GET /auth/recovery-email/verify`와 `GET /settings/recovery-email/verify`는 React 확인 화면으로 연결만 수행하고, 실제 상태 변경은 `GET /api/recovery-email/preview`와 `POST /api/recovery-email/confirm`에서 처리한다
- 비로그인용 `POST /auth/recovery-email/resend`는 두지 않고, 재발송은 인증된 현재 사용자 기준의 `POST /api/settings/recovery-email/resend` 하나로 통일한다
- `POST /api/settings/recovery-email/resend`는 현재 사용자 본인의 가장 최근 미소비 verification flow target에만 동작하고, target이 없으면 안전한 no-op 또는 안내 응답을 반환한다
- recovery email 관련 엔드포인트는 `POST /api/settings/recovery-email`과 `POST /api/settings/recovery-email/resend`에 대해 최소 `계정당 5회/1시간`, `IP당 10회/1시간` 제한을 적용한다
- 새 verification token을 발급할 때는 같은 계정과 같은 `purpose`의 기존 미사용 token을 먼저 무효화한다
- recovery email 변경 요청은 검증 성공 전까지 기존 `member.recovery_email`을 유지하고, 성공 시점에만 `target_email`을 반영한다
- `target_email`이 다른 계정의 검증 완료 `member.recovery_email`로 이미 승격된 경우에는 확인 요청을 실패 처리하고 token을 무효화한다
- 세션 쿠키는 `HttpOnly`, 운영 환경에서는 `Secure`, `SameSite=Lax`를 기본으로 한다
- 세션 고정 공격 방지를 위해 로그인 시 세션 재발급 정책을 사용한다
- 로그아웃 시 세션 무효화와 인증 쿠키 삭제를 함께 수행한다
- same-origin fetch/JSON 요청도 일반 폼 요청과 동일하게 인증과 CSRF 보호를 받아야 한다
- 로그인 실패 응답은 존재하지 않는 계정과 비밀번호 오류를 구분하지 않는 공통 메시지로 처리한다
- 단일 서버 v1에서는 `SessionRegistry` 또는 동등한 세션 관리로 `PENDING_DELETE`/`DISABLED` 전환 시 기존 세션을 즉시 expire 대상으로 표시한다
- 삭제 요청 취소는 `delete_due_at` 이전에만 허용하고, 취소 후에는 기존 세션을 재사용하지 않고 새 로그인만 허용한다

### 26.6 데이터 조회 / 권한 강제 규칙

- 사용자 식별은 항상 서버 세션에서 가져오고, 일반 사용자 요청에서 `memberId`를 입력으로 받지 않는다
- 소유권이 필요한 조회는 `findByIdAndMemberId`, `existsByIdAndMemberId` 형태로 쿼리 단계에서 제한한다
- `findById(id)`로 먼저 가져온 뒤 소유자 비교만 하는 방식보다, 애초에 `memberId` 조건을 같이 넣는 방식을 우선한다
- 컨트롤러, 서비스, 리포지토리 중 한 층만 믿지 않고 서비스와 리포지토리에서 모두 소유권 전제를 유지한다
- 대시보드, 캘린더, 검색 결과도 모두 `member_id` 조건을 포함해야 한다
- 태그 자동완성, 태그 칩, 태그 필터 후보 조회도 항상 현재 로그인 사용자 `member_id` 조건을 포함해야 한다
- 스니펫 검색의 `q`, `tag` 조합 조회도 항상 현재 로그인 사용자 범위 안에서만 수행하고, `tag`는 `normalized_name` 기준 단일 정확 일치로 해석한다
- 스니펫 저장 시 태그 조회/생성도 현재 사용자 범위에서만 수행하고, 다른 사용자의 태그 id를 재사용하지 않는다
- `snippet_tag`는 `member_id`와 복합 foreign key로 같은 사용자 범위의 snippet/tag 연결만 허용하도록 DB 레벨에서 강제한다

### 26.7 화면 렌더링 / React 라우팅 규칙

- 사용자 화면은 React Router 기반 `/app` 경로에서 렌더링한다
- 보호 화면 진입 전에는 세션 상태를 확인하고, 비로그인 상태면 `/app/login`으로 보낸다
- 성공/실패 메시지는 로컬 상태, 배너, 토스트, 쿼리스트링 같은 SPA 친화적 방식으로 전달한다
- 화면에서 발생한 데이터 변경은 same-origin API 응답을 store에 즉시 반영해 후속 재조회 비용을 줄인다
- 시크릿 경고처럼 사용자의 재확인이 필요한 흐름은 API `409`와 경고 DTO를 사용하고, 화면은 현재 입력 상태를 유지한 채 재확인 UI를 보여준다
- 공통 레이아웃, 사이드바, 카드, 폼, 상태 배지는 React 컴포넌트 단위로 재사용 가능하게 구성한다

### 26.8 Markdown / HTML 안전 처리 기준

- Markdown은 서버에서 HTML로 렌더링한다
- 렌더링 후에는 허용 목록 기반 HTML Sanitizer를 반드시 거친다
- 허용 태그는 문단, 제목, 목록, 코드 블록, 링크, 강조 정도로 제한한다
- 임의의 `script`, `style`, 인라인 이벤트 핸들러는 모두 제거한다
- 외부 이미지 임베드는 v1에서 비활성화하거나 엄격히 제한한다
- 새 창 링크를 허용할 경우 `rel="noopener noreferrer"`를 강제한다

### 26.9 예외 처리 / 사용자 메시지 규칙

- 전역 예외 처리기를 둔다
- 검증 실패는 사용자에게 필드별 오류 메시지로 보여준다
- 권한 없는 리소스 접근은 페이지 요청이면 오류 페이지 또는 안전한 안내 화면, API 요청이면 `403/404`로 처리한다
- 동일 날짜 출결 중복은 `409` 성격의 비즈니스 오류로 다룬다
- 시크릿 경고가 필요한 스니펫 저장은 `409`와 `SnippetSecretWarningResponse`로 응답한다
- 예상하지 못한 서버 오류는 상세 스택을 화면에 노출하지 않고, 사용자에게는 일반 메시지와 재시도 안내만 제공한다

### 26.10 DB 인덱스 / 마이그레이션 기준

- `member.username`에 unique index
- `member.recovery_email`에 unique index를 두되, `NULL`은 허용하고 검증 완료로 승격된 non-null 값만 전역 unique 대상으로 취급한다
- `recovery_email_verification_token.token_hash`에 unique index
- `recovery_email_verification_token(member_id, purpose, invalidated_at, expires_at)` 인덱스를 추가해 활성 token 조회와 무효화 처리를 단순화한다
- `recovery_email_verification_token(expires_at)` 인덱스를 추가해 만료 token 정리 작업을 단순화한다
- `attendance_record(member_id, attendance_date)`에 unique index
- `attendance_record(member_id, attendance_date, status)` 보조 인덱스 검토
- `attendance_audit_log(member_id, changed_at)` 인덱스 추가
- `attendance_audit_log(attendance_record_id, changed_at)` 인덱스 추가
- `member(status, delete_due_at)` 인덱스를 추가해 삭제 대상 계정 조회와 배치 삭제를 단순화한다
- `snippet(member_id, updated_at)` 인덱스 추가
- `snippet(id, member_id)` unique index
- `tag(member_id, normalized_name)`에 unique index
- `tag(id, member_id)` unique index
- `snippet_tag(member_id, snippet_id, tag_id)` 기본 키를 사용한다
- 태그 필터 조회 최적화가 필요하면 `snippet_tag(member_id, tag_id, snippet_id)` 보조 인덱스를 추가한다
- Flyway는 `V1__baseline.sql`부터 시작하고, 스키마 변경은 롤포워드 방식으로만 관리한다
- 운영 DB에 직접 수동 수정한 뒤 나중에 마이그레이션 파일을 맞추는 방식은 금지한다

### 26.11 설정 / 프로필 / 환경변수 기준

- 최소 프로필은 `local`, `prod` 두 개를 둔다
- `local`에서는 개발 편의를 위해 Docker Compose MySQL을 사용하고, `prod`는 EC2 운영 값을 사용한다
- 최소 환경변수 예시: `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_TIMEZONE`, `AWS_REGION`, `BACKUP_S3_BUCKET`, `SESSION_COOKIE_NAME`, `MAIL_FROM_ADDRESS`, `APP_BASE_URL`
- 애플리케이션, 컨테이너, DB 세션에 `Asia/Seoul`을 명시적으로 맞춘다
- 시크릿은 `.env` 또는 시스템 환경변수로 주입하고 저장소에 커밋하지 않는다
- 운영 환경의 시크릿 파일이나 환경변수 소스는 최소 권한 파일 권한으로 보호한다
- 가능하면 AWS SSM Parameter Store 또는 Secrets Manager를 우선 검토한다
- 향후 첨부 파일이나 정적 자산 버킷이 생기면 `ASSET_S3_BUCKET`처럼 목적별 환경변수로 분리한다

### 26.12 테스트 전략

- 장려금 계산 규칙은 단위 테스트로 고정한다
- 날짜 경계와 KST 변환 규칙을 테스트한다
- 회원가입 시 recovery email 인증 메일 발송과 검증 완료 처리를 테스트한다
- 회원가입 직후 `member.recovery_email`이 즉시 저장되지 않고, 검증 성공 시점에만 반영되는지 테스트한다
- 미검증 recovery email target이 다른 계정의 가입/변경을 전역 unique 수준에서 선점하지 않는지 테스트한다
- 회원가입 엔드포인트가 최소 `IP당 5회/1시간` 제한 또는 동등한 완화 정책을 적용하는지 테스트한다
- 이메일 보안 스캐너나 프리패처가 `GET /auth/recovery-email/verify`를 먼저 호출해도 실제 검증 완료는 `POST` 확인 전까지 일어나지 않는지 테스트한다
- recovery email 재발송 시 같은 계정과 같은 목적의 이전 미사용 token이 무효화되는지 테스트한다
- 만료된 token이 있더라도 현재 계정의 가장 최근 미소비 verification flow target으로 재발송이 가능한지 테스트한다
- 이미 사용된 token, 만료된 token, 무효화된 token이 다시 소비되지 않는지 테스트한다
- 다른 이메일 또는 다른 목적에 발급된 token으로 검증 완료 처리할 수 없는지 테스트한다
- recovery email 변경 검증이 완료되기 전까지 기존 `member.recovery_email`이 유지되는지 테스트한다
- 다른 계정이 먼저 같은 이메일을 검증 완료한 경우 뒤늦은 token 확인이 실패하고 무효화되는지 테스트한다
- 검증되지 않은 `recovery_email` 계정은 원격 비밀번호 초기화/삭제 요청 대상에서 제외되는지 테스트한다
- `POST /api/settings/recovery-email/resend`가 현재 로그인 사용자 본인의 pending target에만 동작하고, 다른 계정 대상 재발송이 불가능한지 테스트한다
- `POST /api/settings/recovery-email/resend`가 `1분` 쿨다운과 `계정당 5회/1시간`, `IP당 10회/1시간` 제한을 지키는지 테스트한다
- 사용자 A/B 격리와 소유권 차단은 서비스 테스트와 MVC 통합 테스트 둘 다 작성한다
- 인증되지 않은 요청의 리다이렉트 동작을 테스트한다
- 출결 중복 등록, 스니펫 소유권 위반, 검색 필터 동작을 테스트한다
- CSRF 토큰 없이 보호된 POST/PATCH/DELETE 요청이 차단되는지 테스트한다
- `POST /auth/login`이 최소 `IP당 10회/10분`, `username당 실패 5회/15분` 제한 또는 동등한 완화 정책을 지키는지 테스트한다
- `POST /api/auth/signup`이 최소 `IP당 5회/1시간` 제한을 지키는지 테스트한다
- 로그인 전후 세션 id 변경 또는 동등한 세션 고정 공격 방지 동작을 테스트한다
- 운영 쿠키 속성 `HttpOnly`, `Secure`, `SameSite` 설정을 검증한다
- 악성 Markdown 입력이 Sanitizer를 거쳐 스크립트 실행 없이 무해화되는지 테스트한다
- 로그인 실패 메시지가 계정 존재 여부를 노출하지 않는지 테스트한다
- 태그 자동완성/필터 결과에 다른 사용자 태그가 섞이지 않는지 테스트한다
- Actuator가 의도한 엔드포인트만 노출하는지 테스트한다
- 프록시 뒤에서 애플리케이션이 신뢰하는 클라이언트 IP가 운영 의도와 일치하는지 수동 점검하거나 통합 테스트로 검증한다
- 삭제 요청 직후 기존 세션으로 보호 페이지를 요청하면 세션이 무효화되고 접근이 차단되는지 테스트한다
- `delete_due_at` 이전 운영자 취소 시 계정이 `ACTIVE`로 복구되고 기존 세션은 재사용되지 않는지 테스트한다
- `지각`과 `조퇴`가 서로 섞여 결석으로 잘못 환산되지 않는지 계산 테스트를 작성한다
- 저장하지 않은 날짜가 자동 결석 처리되지 않는지 테스트한다
- 동일 날짜 출결 이중 제출 시 `500`이 아니라 의도한 비즈니스 응답으로 종료되는지 테스트한다
- 출결 메모가 저장되더라도 월 통계와 장려금 계산 결과는 변하지 않는지 테스트한다
- 출결 생성/수정/삭제 시 감사 로그가 기대한 before/after 값으로 남는지 테스트한다
- 계정 purge 대상이 정의된 순서로 정리되고 마지막에 `member`가 삭제되는지 테스트한다
- 계정 purge 재실행 시 이미 일부 정리된 항목 때문에 비정상 종료하지 않는지 테스트한다
- 계정 purge 시 남아 있는 recovery email verification token 레코드도 함께 정리되는지 테스트한다
- 계정 purge 후 `attendance_audit_log`가 정책대로 비식별화되거나 정리되는지 테스트한다
- `snippet_tag`가 다른 사용자 소유 `snippet`/`tag` 조합을 DB 레벨에서 거부하는지 통합 테스트한다
- 스니펫 검색에서 `q`만 있을 때, `tag`만 있을 때, `q + tag` 조합일 때 결과가 기대대로 나오는지 테스트한다
- 스니펫 검색에서 다른 사용자 소유 태그로 필터링할 수 없는지 테스트한다
- 스니펫 검색에서 검색 조건이 없을 때 최신 수정순 목록이 반환되는지 테스트한다
- 스니펫 검색 결과가 최대 `50건`으로 제한되는지 테스트한다
- 시크릿 패턴 감지 시 1차 요청은 저장되지 않고 경고 응답이 반환되며, 서버가 발급한 token과 같은 content fingerprint로 재요청했을 때만 저장되는지 테스트한다
- 임의로 만든 token, 만료된 token, 다른 세션이나 다른 내용의 token으로는 저장되지 않는지 테스트한다
- 백업 복원 리허설에서 삭제 요청 등록부 기준 수동 scrub 후 삭제 요청 계정이 되살아나지 않는지 수동 점검한다
- 테스트/디버깅용 운영 데이터 사본 생성 절차가 허용 목록 밖의 필드를 제거하는지 점검한다
- 최소 수동 검증 시나리오도 문서화한다

핵심 수동 검증 예시:

- 사용자 A 로그인 후 대시보드 진입
- 회원가입 직후 recovery email 인증 링크 수신 및 인증 완료 확인
- recovery email 검증 링크 `GET` 진입 후 확인 화면을 거쳐 `POST`로 최종 인증되는지 확인
- 사용자 B 로그인 후 A와 다른 데이터 표시 확인
- A가 B의 스니펫 상세 URL 직접 접근 시 차단 확인
- 출결 저장 후 대시보드 요약 즉시 반영 확인
- 동일 날짜 출결 저장 버튼 연속 클릭 시 중복/500 없이 처리 확인
- 삭제 요청 처리된 테스트 계정이 기존 세션으로 대시보드 재접근 시 즉시 로그아웃되는지 확인
- 스니펫에 테스트용 키 패턴 입력 시 1차 저장 차단과 2차 확인 후 저장을 모두 확인
- 백업 파일 생성 및 복원 리허설 확인

### 26.13 구현 우선순위

- 1순위: 인증, 세션, 사용자 격리, 출결 CRUD, 장려금 계산
- 2순위: 대시보드 조합 조회, 스니펫 CRUD, 태그 필터, Markdown 안전 렌더링
- 3순위: 화면 디테일, 운영 알람, 백업 자동화
- 구현 중 우선순위가 충돌하면 `예쁜 기능`보다 `권한/정합성/복구 가능성`을 우선한다

## 27. 최종 결정 요약

- API 샘플이 아니라 실제 웹제품으로 만든다
- Spring Boot 모놀리식 + React `/app` 프론트 + same-origin JSON API 구조를 사용한다
- AWS 단일 클라우드로 배포한다
- 모바일과 데스크톱을 모두 신경 쓰는 반응형 실사용 서비스로 만든다
- v1은 반드시 끝낼 수 있을 정도로 작게 유지하되, 실제로 쓸 수 있을 정도로 충분히 유용해야 한다
- v1 첫 배포 인스턴스는 `t3.small`로 시작하고, 운영 안정화 후 `t4g.small` 전환을 검토한다
