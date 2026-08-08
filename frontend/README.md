# 모이Go 프론트엔드

모이Go(여행 계획 공동 작성 서비스)의 프론트엔드입니다. **Next.js(App Router) + TypeScript + Tailwind CSS**로 만들어졌고,
아직 백엔드 API가 없어 `src/lib/api/*`가 mock 데이터를 리턴하는 구조로 전체 화면이 동작합니다.

## 로컬 실행

```bash
git clone https://github.com/Groom-team2-project/Groom-Semi3-Project-Team2.git
cd Groom-Semi3-Project-Team2
git checkout develop

cd frontend
npm install
npm run dev
```

브라우저에서 [http://localhost:3000](http://localhost:3000) 접속하면 됩니다.

배포 전 빌드가 깨지지 않는지 확인하려면:

```bash
npm run build
```

린트 확인:

```bash
npm run lint
```

## 환경변수

```bash
cp .env.example .env.local
```

지금은 `.env.local`의 `NEXT_PUBLIC_API_BASE_URL`을 **비워둔 채로 두면 됩니다.** 이 값이 비어있는 동안
`src/lib/api/*`의 모든 함수는 실제 fetch 대신 mock 데이터를 리턴합니다 (`src/lib/api/client.ts`의
`USE_MOCK`은 `!API_BASE_URL`로 자동 계산돼서, 이 값을 채우면 따로 코드를 안 고쳐도 자동으로 꺼집니다).
백엔드가 준비되면 이 값에 API 서버 주소를 채우고, `src/lib/api/*` 내부 구현만 함수 단위로 하나씩 fetch로
바꾸면 됩니다.

## 브랜치 전략

- 항상 `develop`에서 본인 담당 기능 브랜치를 따서 작업하세요. (예: `feature/vote-api`, `feature/schedule-comments`)
- `develop`에 직접 커밋하지 마세요.
- 작업이 끝나면 `develop`을 대상으로 PR을 생성하세요. 리뷰어 1명 이상 승인을 받고, PR을 올리기 전에
  로컬에서 `npm run build`, `npm run lint`가 통과하는지 확인해주세요.
- `develop`에 merge되면 Netlify가 자동으로 재배포합니다.
  배포 주소: **https://moigo.netlify.app/**

## 폴더 구조

| 경로 | 역할 |
|---|---|
| `src/app` | 라우트(페이지). App Router 기준 실제 URL 구조와 폴더 구조가 그대로 대응됩니다. |
| `src/components/ui` | 공용 UI 컴포넌트 (Button, Card, AppBar, BottomTabBar, Tag, FieldInput 등). 도메인에 상관없이 재사용. |
| `src/components/plan` | 도메인 컴포넌트 (PlanCard, TimelineStop, VoteCard, MemberRow, ActivityRow 등). 여행 계획 도메인에 특화된 컴포넌트. |
| `src/lib/api` | mock/API 함수 모음. 화면은 이 안의 함수(`getPlans`, `createSchedule`, `castVote` 등)만 호출하고, 실제 데이터 소스(mock ↔ fetch)는 이 폴더 안에서만 갈아끼웁니다. |
| `src/lib/hooks` | 화면 여러 곳에서 재사용하는 커스텀 훅 (`usePlan` 등). |
| `src/lib` (그 외) | `utils.ts`(날짜/D-day 포맷 등), `formDraft.ts`(폼 임시저장), `pickedPlace.ts`(장소 검색 결과 전달), `lastPlan.ts` 등 화면 간 상태 연결용 유틸. |
| `src/context` | 전역 상태. 지금은 로그인 상태를 관리하는 `AuthContext`(`useAuth()`)가 있습니다. |

## 새 화면/컴포넌트 작업 전에

**[`AGENTS.md`](./AGENTS.md)** 를 먼저 확인하세요. UI 일관성 규칙(기존 컴포넌트 재사용, 디자인 토큰 사용),
실제 색상/폰트/여백 토큰 값, 담당 도메인별로 어떤 파일을 고쳐야 하는지 정리된 "폴더/라우트 지도"가
들어있습니다.

## mock 데이터 → 실제 API 연동

`src/lib/api/*.ts` 안의 함수 시그니처(이름·파라미터·리턴 타입)는 그대로 두고, **함수 내부 구현만** mock
(`store.ts` 조작)에서 실제 `fetch` 호출로 바꾸면 됩니다. 화면(컴포넌트) 쪽 코드는 `await getPlans()`처럼
함수만 호출하고 있어서 건드릴 필요가 없습니다. 새로 API를 붙일 때도 컴포넌트에서 직접 `fetch`를 호출하지
말고, 반드시 `src/lib/api` 안에 함수로 추가해주세요. 자세한 규칙은 `AGENTS.md`의 "mock → 실제 API 전환
규칙" 섹션을 참고하세요.
