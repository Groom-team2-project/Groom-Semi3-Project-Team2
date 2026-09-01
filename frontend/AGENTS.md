<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

---

# 모이Go 프론트엔드 작업 가이드

여러 명(팀원 + AI 에이전트)이 동시에 `/frontend`를 건드리게 됩니다. 이 문서는 "어디를 고쳐야 하는지"와
"어떻게 고쳐야 기존 화면들과 톤이 안 흐트러지는지"를 정리한 것입니다. 화면/컴포넌트 작업을 시작하기 전에
작업 전 아래 기준을 확인합니다.

## UI 일관성 규칙

- 새 화면·컴포넌트를 만들기 전에 **`src/components/ui/*`를 먼저 확인**하고, 있는 컴포넌트는 그대로 재사용
  하세요. 버튼·카드·태그·인풋·아바타·앱바·하단탭바는 전부 이미 있습니다 (아래 표 참고).
- 필요한 스타일이 기존 컴포넌트에 없다면, **새 컴포넌트를 새로 만들지 말고 기존 컴포넌트에 variant/prop을
  추가**하는 방식으로 확장하세요. (예: `Button`에 새 색상이 필요하면 `Button.tsx`의 `Variant` 타입과
  `VARIANT_CLASS`에 항목을 추가 — 새 `MyButton.tsx`를 만들지 않기)
- 색·폰트·여백은 아래 "디자인 토큰" 표의 Tailwind 클래스를 우선 사용합니다.
  (`bg-[#3182F6]` 대신 `bg-primary`)
- 새 페이지는 기존 페이지의 레이아웃 패턴을 그대로 따르세요:
  - 계획 상세 하위 화면: `<div className="flex min-h-dvh flex-col"><AppBar .../><div className="flex flex-1 flex-col gap-3 px-4 pb-8">...내용...</div></div>`
  - 하단 탭바가 필요한 화면(홈/일정/투표/내정보 4개)만 `<BottomTabBar planId={...} />`를 맨 아래에 추가
  - 리스트류 화면은 `gap-3`, 폼 화면은 `Field` + `FieldInput`/`FieldTextarea` 조합 사용
- `"use client"`는 브라우저 상태(useState/useEffect)·세션·토큰·클릭 같은 이벤트가 실제로 필요한 페이지에만
  붙이세요. 현재 계획·댓글·활동 기록 화면은 클라이언트에서 `src/lib/api`를 호출하므로
  `"use client"` + `use(params)` / `use(searchParams)` 패턴을 사용합니다. params/searchParams가 Promise로
  오는 Next 16 App Router 컨벤션을 지키세요. 순수 목록·정적 페이지는 서버 컴포넌트로 두고 `await params` /
  `await searchParams`를 쓰는 것도 고려하세요.
- `<Link>`/`Button href=...`는 내부적으로 `<a>` 태그입니다. 전역 기본 링크색(`globals.css`의
  `@layer base { a { color: var(--color-gray-500); } }`)이 있으니, 강조가 필요한 링크는 반드시
  `text-primary` 등 명시적 색상 클래스를 직접 줘야 합니다 (기본값에 기대지 말 것).
- API 호출로 화면 상태가 바뀌는 생성·수정·삭제 UI에는 반드시 `pending` 상태와 실패 안내를 함께 구현하세요.
  오류는 `ApiError.status`를 기준으로 구분하고, 페이지 이동 대신 현재 화면에서 `Toast`로 안내하는 것을 기본으로
  합니다. 인증 필요(401), 권한 없음(403), 삭제·미존재(404), 입력 오류(400), 서버 오류(5xx)를 하나의 메시지로
  뭉뚱그리지 마세요.

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
- API: `src/lib/api/places.ts` (`searchPlacesByKeyword`, `searchPlacesNearby`, `getSavedPlaces`, `addPlaceToPlan`, `removePlaceFromPlan`)
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
- API: `src/lib/api/activities.ts` (`getActivities`, `getMyActivities`)
- 목록은 `createdAt + logId` 복합 커서와 `hasNext`를 사용합니다. 다음 페이지 요청은 반드시 두 커서를 함께
  전달하고, 기존 목록 뒤에 append하세요. 페이지 번호·OFFSET 방식으로 바꾸지 마세요.
- 활동 기록 생성은 백엔드 각 도메인에서 `ActivityLogService.record()`를 호출해 처리합니다. 프론트는
  `store.recordActivity()` 대신 조회 API 응답을 기준으로 화면을 갱신합니다.
- 타입: `ActivityLog`

### 계획 자체 (plans) / 인증
- 화면: `src/app/plans/page.tsx`(목록), `src/app/plans/new/page.tsx`(생성),
  `src/app/plans/[planId]/page.tsx`(계획 홈), `src/app/login/page.tsx`, `src/app/profile/page.tsx`
- API: `src/lib/api/plans.ts`, `src/lib/api/auth.ts`
- 인증 상태: `src/context/AuthContext.tsx` (`useAuth()` — `user`, `isLoading`, `loginWithKakao`, `logout`)
- 계획 상세 공통 로딩: `src/lib/hooks/usePlan.ts` (`usePlan(planId)` — `{ plan, isLoading, refresh }`)
- 마지막으로 본 계획 기억(프로필 등에서 하단 탭바 동작용): `src/lib/lastPlan.ts`

## API·mock 전환 규칙 (`src/lib/api/*`)

- API 함수는 **`async function`이고 `Promise`를 리턴**하며, 페이지·컴포넌트에서는 직접 `fetch`하지 않습니다.
  서버 연동은 `src/lib/api/*.ts`의 함수 내부에서 `apiFetch`로 수행하고, 응답 DTO는 그 안에서 화면 타입으로 매핑하세요.
- 기본 실행은 실제 API입니다. `API_BASE_URL`이 빈 문자열이면 Netlify rewrite를 통한 같은 출처 `/api` 호출을
  의미할 수 있습니다.
- 목 데이터는 `NEXT_PUBLIC_USE_MOCK=true`일 때만 사용합니다. 목 분기를 새로 추가하거나 유지할 때도 이 플래그를
  기준으로 판단합니다.
- 목 구현은 화면 초기 개발 또는 백엔드 미구현 영역을 위한 임시 수단입니다. 실제 API가 준비된 도메인은 서버 응답을
  화면에 반영합니다.
- 함수 시그니처는 백엔드 전환 전후에도 유지합니다. 예시는 다음과 같습니다.

  ```ts
  // src/lib/api/votes.ts
  export async function getVotes(planId: string): Promise<Vote[]>
  export async function getVote(planId: string, voteId: string): Promise<Vote | null>
  export async function createVote(planId: string, input: CreateVoteInput): Promise<Vote>
  export async function castVote(planId: string, voteId: string, optionId: string): Promise<Vote>
  ```

- 목록 조회는 `getXxx(planId)`, 단건 조회는 `getXxx(planId, id)` (404는 `null`로 변환할 수 있음),
  생성은 `createXxx(planId, input)`, 수정은 `updateXxx(planId, id, input)`, 삭제는
  `deleteXxx(planId, id): Promise<void>` 컨벤션을 따릅니다.

`apiFetch`는 인증 토큰을 요청 헤더에 자동 첨부하고, 401이면 토큰 재발급을 한 번 시도한 뒤 `ApiError`를
던집니다. 토큰 저장·재발급은 `apiFetch`의 공통 흐름으로 관리합니다.

## 도메인 경계와 협업 규칙

- 담당 도메인이 아닌 백엔드 API·엔티티·DB 마이그레이션은 담당자와 API 계약을 합의한 뒤 반영합니다. 화면에서
  문제가 보이면 재현 조건과 필요한 API 계약을 담당자에게 공유합니다.
- 도메인 간 연결이 필요하면 프론트 타입·API 함수의 입력/응답 형식, 권한, 삭제된 대상 처리 방식을 먼저 합의하세요.
  예를 들어 댓글은 일정이 존재하는지 백엔드에서 검증하고, 활동 기록은 변경이 성공한 뒤 백엔드가 남깁니다.
- 다른 도메인 화면은 공용 UI 컴포넌트·레이아웃·오류 표현처럼 프론트 공통 범위에서 개선합니다. 도메인 데이터
  저장 방식은 담당자와 합의한 API 계약을 기준으로 유지합니다.

## 작업 전 체크리스트

1. `src/components/ui/*` 전체를 먼저 훑어보고, 내가 만들려는 화면에 재사용할 수 있는 컴포넌트가 있는지 확인
2. 담당 도메인 폴더(위 "폴더/라우트 지도" 참고)의 기존 화면 1~2개를 열어서 레이아웃 패턴 확인
3. 관련 `src/lib/api/*.ts`의 기존 함수 시그니처/네이밍 컨벤션을 확인하고 동일한 스타일로 추가
4. 색상/여백은 위 "디자인 토큰" 표에 있는 Tailwind 클래스만 사용 (임의값·인라인 style 지양)
5. 작업 후 `npx tsc --noEmit`, `npm run lint` 통과 확인. 환경상 `npm run build`가 실패하면 코드 오류와
   Turbopack/실행 환경 오류를 구분해 보고하세요.
