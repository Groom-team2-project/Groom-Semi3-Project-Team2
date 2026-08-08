<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

---

# 모이Go 프론트엔드 작업 가이드

여러 명(팀원 + AI 에이전트)이 동시에 `/frontend`를 건드리게 됩니다. 이 문서는 "어디를 고쳐야 하는지"와
"어떻게 고쳐야 기존 화면들과 톤이 안 흐트러지는지"를 정리한 것입니다. 화면/컴포넌트 작업을 시작하기 전에
반드시 읽어주세요.

## UI 일관성 규칙

- 새 화면·컴포넌트를 만들기 전에 **`src/components/ui/*`를 먼저 확인**하고, 있는 컴포넌트는 그대로 재사용
  하세요. 버튼·카드·태그·인풋·아바타·앱바·하단탭바는 전부 이미 있습니다 (아래 표 참고).
- 필요한 스타일이 기존 컴포넌트에 없다면, **새 컴포넌트를 새로 만들지 말고 기존 컴포넌트에 variant/prop을
  추가**하는 방식으로 확장하세요. (예: `Button`에 새 색상이 필요하면 `Button.tsx`의 `Variant` 타입과
  `VARIANT_CLASS`에 항목을 추가 — 새 `MyButton.tsx`를 만들지 않기)
- 색·폰트·여백은 **하드코딩 금지**, 아래 "디자인 토큰" 표에 있는 값만 Tailwind 클래스로 사용하세요.
  (`bg-[#3182F6]` 같은 임의값 대신 `bg-primary`)
- 새 페이지는 기존 페이지의 레이아웃 패턴을 그대로 따르세요:
  - 계획 상세 하위 화면: `<div className="flex min-h-dvh flex-col"><AppBar .../><div className="flex flex-1 flex-col gap-3 px-4 pb-8">...내용...</div></div>`
  - 하단 탭바가 필요한 화면(홈/일정/투표/내정보 4개)만 `<BottomTabBar planId={...} />`를 맨 아래에 추가
  - 리스트류 화면은 `gap-3`, 폼 화면은 `Field` + `FieldInput`/`FieldTextarea` 조합 사용
- `"use client"`는 브라우저 상태(useState/useEffect)·세션·토큰·클릭 같은 이벤트가 실제로 필요한 페이지에만
  붙이세요. 이 프로젝트는 지금 전부 mock API를 클라이언트에서 `useEffect`로 호출하는 구조라 모든
  `src/app/plans/[planId]/**/page.tsx`가 `"use client"` + `use(params)` / `use(searchParams)` 패턴을
  쓰고 있지만(params/searchParams가 Promise로 오는 Next 16 App Router 컨벤션), 서버에서 미리 데이터를
  내려줘도 되는 순수 목록/정적 페이지를 새로 만든다면 서버 컴포넌트로 두고 `await params` /
  `await searchParams`를 쓰는 것도 고려하세요. 클라이언트 번들이 줄어듭니다.
- `<Link>`/`Button href=...`는 내부적으로 `<a>` 태그입니다. 전역 기본 링크색(`globals.css`의
  `@layer base { a { color: var(--color-gray-500); } }`)이 있으니, 강조가 필요한 링크는 반드시
  `text-primary` 등 명시적 색상 클래스를 직접 줘야 합니다 (기본값에 기대지 말 것).

## 디자인 토큰 (`src/app/globals.css` 기준 실제 값)

| Tailwind 클래스 | CSS 변수 | 값 | 용도 |
|---|---|---|---|
| `bg-primary` / `text-primary` | `--color-primary` | `#3182F6` | 포인트 블루 (버튼, 강조 텍스트) |
| `bg-primary-dark` / `text-primary-dark` | `--color-primary-dark` | `#1B64DA` | 포인트 블루 진한 톤 |
| `bg-primary-soft` / `text-primary-soft` | `--color-primary-soft` | `#EAF2FF` | 블루 배경(뱃지, 배너, 선택 상태) |
| `text-ink` | `--color-ink` | `#191F28` | 기본 본문 텍스트 |
| `text-gray-700` | `--color-gray-700` | `#4E5968` | 보조 텍스트 |
| `text-gray-500` | `--color-gray-500` | `#8B95A1` | 메타 텍스트, 링크 기본색, 비활성 탭 |
| `border-gray-300` | `--color-gray-300` | `#D1D6DB` | 스크롤바 등 옅은 보더 |
| `border-gray-200` | `--color-gray-200` | `#E5E8EB` | 카드/구분선 기본 보더 |
| `bg-gray-100` | `--color-gray-100` | `#F2F4F6` | 페이지 배경, 인풋 배경 |
| `bg-red` / `text-red` | `--color-red` | `#F04452` | 위험(삭제) |
| `bg-red-soft` | `--color-red-soft` | `#FDECEE` | 위험 배경 |
| `bg-orange` / `text-orange` | `--color-orange` | `#FF9F1C` | 투표/마감 임박 강조 |
| `bg-orange-soft` | `--color-orange-soft` | `#FFF4E5` | 투표 배너/뱃지 배경 |
| `bg-green` | `--color-green` | `#00C896` | 보조 강조(아바타 색 등) |
| `bg-kakao` / `text-kakao-ink` | `--color-kakao` / `--color-kakao-ink` | `#FEE500` / `#191600` | 카카오 버튼 전용 |
| `font-sans` (기본값) | `--font-sans` | Pretendard, Apple SD Gothic Neo, system-ui... | 기본 폰트 |
| `font-mono` | `--font-mono` | SFMono-Regular, Consolas, Menlo... | 시간/코드성 텍스트 |
| — | `--shadow-card` | `0 1px 2px rgba(25,31,40,.04), 0 8px 24px -12px rgba(25,31,40,.18)` | 카드/시트 그림자 (`.app-shell`에 적용됨) |

레이아웃 여백은 대부분 `px-4`(본문 좌우), `gap-3`(리스트 항목 간), `rounded-2xl`(카드),
`rounded-xl`(인풋/작은 카드), `rounded-full`(뱃지/탭/알약형 버튼) 로 통일되어 있습니다. 새 UI를 만들 때
이 값들을 그대로 따라주세요.

## 공용 컴포넌트 (`src/components/ui/*`)

| 컴포넌트 | 파일 | 역할 / 주요 props |
|---|---|---|
| `Button` | `Button.tsx` | 공용 버튼. `variant: "primary" \| "ghost" \| "soft" \| "kakao" \| "danger"`, `size: "md" \| "sm"`, `fullWidth`, `href`(주면 `next/link`로 렌더링) |
| `Card` | `Card.tsx` | 둥근 테두리 카드 컨테이너. `href` 또는 `onClick` 주면 클릭 가능한 카드로 렌더링 |
| `AppBar` | `AppBar.tsx` | 화면 상단 바. `title`, `subtitle`, `backHref`(주면 `‹` 뒤로가기), `actions`(우측 버튼 슬롯) |
| `BottomTabBar` | `BottomTabBar.tsx` | 홈/일정/투표/내정보 하단 탭. `planId` prop 필요 (없으면 홈·일정·투표 탭이 `/plans`로 감) |
| `Avatar`, `AvatarStack` | `Avatar.tsx` | 이니셜 원형 아바타. `size: "xs" \| "sm" \| "md" \| "lg"`, `label`(이니셜 대신 텍스트, 예: `"+1"`) |
| `Tag`, `Chip` | `Tag.tsx` | `Tag`는 작은 알약형 뱃지(`color: "blue" \| "gray" \| "orange"`), `Chip`은 헤더용 큰 알약 |
| `Field`, `FieldInput`, `FieldTextarea` | `FieldInput.tsx` | 폼 라벨+인풋 조합. `Field`로 라벨 감싸고 안에 `FieldInput`/`FieldTextarea` 배치 |
| `Segmented` | `Segmented.tsx` | 세그먼트 컨트롤(탭 전환). 제네릭 `value`/`onChange`, `options: {value, label}[]` |
| `EmptyState` | `EmptyState.tsx` | 빈 목록 상태 표시. `emoji`, `title`, `description` |

## 도메인 컴포넌트 (`src/components/plan/*`)

| 컴포넌트 | 파일 | 역할 |
|---|---|---|
| `PlanCard` | `PlanCard.tsx` | 계획 목록의 카드 1개 (커버/제목/기간/아바타 스택/D-day) |
| `PlanNotFound` | `PlanNotFound.tsx` | 존재하지 않는 planId 접근 시 공용 안내 화면 |
| `DayTabs` | `DayTabs.tsx` | 타임라인의 Day1/Day2... 탭 |
| `TimelineStop` | `TimelineStop.tsx` | 타임라인 한 줄(시간 + 장소 박스). 투표중 일정이면 주황 배경 |
| `PlaceRow` | `PlaceRow.tsx` | 장소 한 줄(썸네일 이모지 + 이름 + 주소). `tag` 또는 `onAdd`(+ 버튼)로 용도 분기 |
| `PlaceSearchTrigger` | `PlaceSearchTrigger.tsx` | "🔍 카카오 장소 검색으로 추가하기" 같은 검색 진입 버튼(링크) |
| `VoteOptionBar`, `VoteListCard` | `VoteCard.tsx` | 투표 옵션 진행률 바 / 투표 목록 카드 |
| `MemberRow` | `MemberRow.tsx` | 멤버 한 줄(아바타 + 이름 + 역할). `editable`이면 역할 변경 `<select>` 렌더 |
| `ActivityRow` | `ActivityRow.tsx` | 활동 로그 한 줄 |
| `CommentItem` | `CommentItem.tsx` | 댓글 한 줄 |

## 폴더/라우트 지도 — 도메인별로 어디를 고쳐야 하는지

### 일정 (schedules)
- 화면: `src/app/plans/[planId]/timeline/page.tsx`(타임라인),
  `src/app/plans/[planId]/timeline/route/page.tsx`(동선 보기),
  `src/app/plans/[planId]/schedules/new/page.tsx`(생성),
  `src/app/plans/[planId]/schedules/[scheduleId]/page.tsx`(상세+댓글),
  `src/app/plans/[planId]/schedules/[scheduleId]/edit/page.tsx`(수정),
  `src/app/plans/[planId]/schedules/[scheduleId]/delete/page.tsx`(삭제 확인)
- 컴포넌트: `src/components/plan/TimelineStop.tsx`, `DayTabs.tsx`
- API: `src/lib/api/schedules.ts` (일정 CRUD + 댓글 CRUD)
- 타입: `src/lib/api/types.ts`의 `Schedule`, `Comment`

### 장소 (places, 카카오 장소 검색)
- 화면: `src/app/plans/[planId]/places/page.tsx`(저장된 장소 목록),
  `src/app/plans/[planId]/places/search/page.tsx`(카카오 키워드/내 주변 검색)
- 컴포넌트: `src/components/plan/PlaceRow.tsx`, `PlaceSearchTrigger.tsx`
- API: `src/lib/api/places.ts` (`searchPlacesByKeyword`, `searchPlacesNearby`, `addPlaceToPlan`)
- 검색 결과를 다른 화면(일정 생성, 투표 생성)으로 되돌려주는 로직: `src/lib/pickedPlace.ts`
  (`setPickedPlace`/`consumePickedPlace`, sessionStorage 기반)
- 타입: `Place`, `PlaceSearchResult`

### 투표 (votes)
- 화면: `src/app/plans/[planId]/votes/page.tsx`(목록),
  `src/app/plans/[planId]/votes/new/page.tsx`(생성),
  `src/app/plans/[planId]/votes/[voteId]/page.tsx`(상세, 투표 참여)
- 컴포넌트: `src/components/plan/VoteCard.tsx` (`VoteOptionBar`, `VoteListCard`)
- API: `src/lib/api/votes.ts` (`getVotes`, `getVote`, `createVote`, `castVote`)
- 타입: `Vote`, `VoteOption`

### 멤버/초대 (members)
- 화면: `src/app/plans/[planId]/members/page.tsx`
- 컴포넌트: `src/components/plan/MemberRow.tsx`
- API: `src/lib/api/members.ts` (`getMembers`, `updateMemberRole`, `getInvitation`, `reissueInvitation`)
- 타입: `Member`, `Invitation`, `Role`

### 활동 이력 (activity log)
- 화면: `src/app/plans/[planId]/activity/page.tsx` (계획 홈의 "최근 활동" 섹션도 같은 API 사용)
- 컴포넌트: `src/components/plan/ActivityRow.tsx`
- API: `src/lib/api/activities.ts` (`getActivities`)
- 활동 기록 생성: `src/lib/api/store.ts`의 `store.recordActivity(...)` — 다른 도메인 API(일정/투표/멤버)에서
  변경이 생길 때 이 메서드를 호출해서 로그를 남김 (예: `schedules.ts`의 `createSchedule` 참고)
- 타입: `ActivityLog`

### 계획 자체 (plans) / 인증
- 화면: `src/app/plans/page.tsx`(목록), `src/app/plans/new/page.tsx`(생성),
  `src/app/plans/[planId]/page.tsx`(계획 홈), `src/app/login/page.tsx`, `src/app/profile/page.tsx`
- API: `src/lib/api/plans.ts`, `src/lib/api/auth.ts`
- 인증 상태: `src/context/AuthContext.tsx` (`useAuth()` — `user`, `isLoading`, `loginWithKakao`, `logout`)
- 계획 상세 공통 로딩: `src/lib/hooks/usePlan.ts` (`usePlan(planId)` — `{ plan, isLoading, refresh }`)
- 마지막으로 본 계획 기억(프로필 등에서 하단 탭바 동작용): `src/lib/lastPlan.ts`

## mock 데이터 패턴 (`src/lib/api/*`)

- 모든 API 함수는 **`async function`이고 `Promise`를 리턴**합니다. 실제 백엔드가 붙기 전까지는 내부에서
  `simulateLatency()`(`store.ts`)로 약간의 지연만 흉내내고, `store`(인메모리 mock DB)를 읽고 씁니다.
- `store.ts`의 `store` 객체가 유일한 "DB"입니다. `store.plans`, `store.schedules`, `store.votes`,
  `store.places`, `store.comments`, `store.activities`, `store.me` 배열/객체를 직접 조작합니다.
- 초기 시드 데이터는 `mockData.ts`에 있습니다. 화면에 보여줄 예시 데이터를 늘리고 싶으면 이 파일에 추가하세요.
- 실제 함수 시그니처 예시 (이 패턴을 그대로 따라서 새 함수를 추가하면 됩니다):

  ```ts
  // src/lib/api/votes.ts
  export async function getVotes(planId: string): Promise<Vote[]>
  export async function getVote(planId: string, voteId: string): Promise<Vote | null>
  export async function createVote(planId: string, input: CreateVoteInput): Promise<Vote>
  export async function castVote(planId: string, voteId: string, optionId: string): Promise<Vote>
  ```

- 목록 조회는 `getXxx(planId)`, 단건 조회는 `getXxx(planId, id)` (없으면 `null` 리턴, throw 하지 않음),
  생성은 `createXxx(planId, input)`, 수정은 `updateXxx(planId, id, input)`, 삭제는
  `deleteXxx(planId, id): Promise<void>` 컨벤션을 따릅니다.

## mock → 실제 API 전환 규칙

- `src/lib/api/*.ts`의 **함수 시그니처(이름/파라미터/리턴 타입)는 그대로 유지**하고, 함수 **내부 구현만**
  mock(`store` 조작)에서 실제 `fetch`/`apiFetch`(`client.ts`)로 교체하세요. 컴포넌트·페이지 쪽 코드는
  건드릴 필요가 없습니다 — 이미 `await getPlans()`, `await createSchedule(...)` 처럼 함수 호출만으로 되어
  있기 때문입니다.
- 공통 fetch 헬퍼는 `src/lib/api/client.ts`의 `apiFetch<T>(path, init)`와 `API_BASE_URL`
  (`process.env.NEXT_PUBLIC_API_BASE_URL`)을 사용하세요. `USE_MOCK`은 `API_BASE_URL`이 비어있는지로
  자동 계산되는 값이라(`!API_BASE_URL`), `.env.local`에 주소를 채우면 따로 코드를 안 고쳐도 자동으로
  `false`가 됩니다. 실제 전환은 이 플래그를 코드에서 분기하는 게 아니라, `src/lib/api/*.ts`의 함수를
  **하나씩** mock → `apiFetch` 호출로 바꿔나가는 방식으로 점진적으로 진행하면 됩니다.
- 새로 API를 연동할 때도 반드시 **`src/lib/api` 안에 함수로 분리**해서 추가하세요. 컴포넌트/페이지에서
  직접 `fetch`를 호출하지 마세요.
- 타입은 `src/lib/api/types.ts` 기준으로 맞추고, 백엔드 응답 필드명이 다르면 API 함수 내부에서 매핑하세요
  (타입 자체를 함수마다 다르게 만들지 말 것).
- `apiFetch`는 아래를 이미 알아서 처리해줍니다. 도메인 API 함수에서 따로 구현하지 마세요.
  - **인증 토큰 자동 첨부**: `localStorage`의 `tripmate_access_token`이 있으면 모든 요청에
    `Authorization: Bearer <토큰>` 헤더를 자동으로 실어 보냅니다. (로그인 API가 실제로 붙으면, 로그인
    성공 시 이 키로 토큰을 저장하도록 구현하면 됩니다)
  - **401 자동 처리**: 응답이 401이면 `/login`으로 강제 이동시키고 `ApiError`를 던집니다.
  - **에러 타입**: 실패하면 항상 `ApiError`(`status`, `message` 포함)를 던집니다. 도메인 함수에서 상태
    코드별로 다르게 처리하고 싶으면 `catch (e) { if (e instanceof ApiError && e.status === 404) ... }`
    형태로 잡으세요.

## 작업 전 체크리스트

1. `src/components/ui/*` 전체를 먼저 훑어보고, 내가 만들려는 화면에 재사용할 수 있는 컴포넌트가 있는지 확인
2. 담당 도메인 폴더(위 "폴더/라우트 지도" 참고)의 기존 화면 1~2개를 열어서 레이아웃 패턴 확인
3. 관련 `src/lib/api/*.ts`의 기존 함수 시그니처/네이밍 컨벤션을 확인하고 동일한 스타일로 추가
4. 색상/여백은 위 "디자인 토큰" 표에 있는 Tailwind 클래스만 사용 (임의값·인라인 style 지양)
5. 작업 후 `npm run build`, `npm run lint` 통과 확인
