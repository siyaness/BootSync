# BootSync Frontend

BootSync 프론트는 Vite + React + TypeScript로 작성되며, 최종 빌드 결과물은 Spring 앱의 `/app` 경로에 정적 자산으로 포함됩니다.

## 실행

개발 서버:

```powershell
cd .\frontend
npm install
npm run dev
```

Spring 앱에 붙일 production build:

```powershell
cd .\frontend
$env:VITE_BASE_PATH='/app/'
npm run build
```

빌드가 끝나면 `dist`가 생성되고, 루트 프로젝트의 Gradle `processResources`가 이를 `build/generated-resources/frontend/static/app`으로 복사합니다.

## 현재 앱 구조

- `src/App.tsx`: 라우트 진입점
- `src/main.tsx`: `BrowserRouter`와 `basename` 설정
- `src/lib/store.tsx`: 세션, 출결, 학습 노트, 설정 API를 다루는 전역 상태
- `src/lib/api.ts`: same-origin API 요청/에러 래퍼
- `src/lib/app-types.ts`: 앱 공용 타입
- `src/lib/display.ts`: 날짜/금액/태그 표시 유틸
- `src/components/layout/AppLayout.tsx`: 공통 레이아웃과 사이드바/하단 탭
- `src/pages/*`: 로그인, 회원가입, 대시보드, 출결, 장려금, 학습 노트, 설정, 이메일 확인 화면

## 참고

- 실제 서비스 화면은 `/app/*`만 사용합니다.
- `src/stories`와 일부 `src/components/ui/*` 파일은 현재 사용자 화면의 핵심 경로는 아니며, UI 참고용 보조 자산 성격으로 남아 있습니다.
