import type {
  ActivityLog,
  Comment,
  Member,
  Place,
  Plan,
  Schedule,
  User,
  Vote,
} from "./types";

/**
 * 화면설계(트립메이트-설계_v2.html)의 예시 데이터를 기반으로 한 초기 mock 데이터.
 * 서버 프로세스/브라우저 세션이 살아있는 동안 store.ts가 이 데이터를 메모리에서
 * 가변 상태로 관리합니다. (새로고침하면 초기화 — 지금 단계에서는 의도된 동작)
 */

const now = Date.now();
const minutesAgo = (m: number) => new Date(now - m * 60_000).toISOString();
const hoursFromNow = (h: number) => new Date(now + h * 3_600_000).toISOString();

export const MOCK_ME: User = {
  id: "user_jieun",
  name: "이지은",
  email: "poseybutter@example.com",
  avatarColor: "#3182F6",
  avatarInitial: "이",
};

const MEMBERS_JEJU: Member[] = [
  { id: "mem_1", userId: "user_jieun", name: "이지은", avatarColor: "#3182F6", avatarInitial: "이", role: "OWNER" },
  { id: "mem_2", userId: "user_minsu", name: "김민수", avatarColor: "#00C896", avatarInitial: "김", role: "EDITOR" },
  { id: "mem_3", userId: "user_seoyeon", name: "박서연", avatarColor: "#FF9F1C", avatarInitial: "박", role: "EDITOR" },
  { id: "mem_4", userId: "user_doyoon", name: "최도윤", avatarColor: "#8B7FF2", avatarInitial: "최", role: "VIEWER" },
];

const MEMBERS_BUSAN: Member[] = [
  { id: "mem_5", userId: "user_jieun", name: "이지은", avatarColor: "#3182F6", avatarInitial: "이", role: "OWNER" },
  { id: "mem_6", userId: "user_minsu", name: "김민수", avatarColor: "#00C896", avatarInitial: "김", role: "EDITOR" },
  { id: "mem_7", userId: "user_seoyeon", name: "박서연", avatarColor: "#FF9F1C", avatarInitial: "박", role: "VIEWER" },
];

const YEAR = new Date().getFullYear();

export const INITIAL_PLANS: Plan[] = [
  {
    id: "plan_jeju",
    title: "제주도 여름 여행 🌊",
    description: "협재 바다 보면서 여유롭게 놀다 오는 4박 5일",
    emoji: "🌊",
    startDate: `${YEAR}-08-14`,
    endDate: `${YEAR}-08-17`,
    ownerId: "user_jieun",
    members: MEMBERS_JEJU,
    createdAt: minutesAgo(60 * 24 * 6),
  },
  {
    id: "plan_busan",
    title: "부산 워케이션",
    description: "일하면서 틈틈이 바다 보러 가는 짧은 워케이션",
    emoji: "🏙️",
    startDate: `${YEAR}-10-03`,
    endDate: `${YEAR}-10-05`,
    capacity: 4,
    ownerId: "user_jieun",
    members: MEMBERS_BUSAN,
    createdAt: minutesAgo(60 * 24 * 2),
  },
];

export const INITIAL_PLACES: Place[] = [
  { id: "place_1", planId: "plan_jeju", name: "협재해변", address: "제주 한림읍 협재리", emoji: "🏖️", source: "KAKAO_LOCAL", usage: ["schedule"] },
  { id: "place_2", planId: "plan_jeju", name: "카페 델문도", address: "협재 뷰 카페", emoji: "☕", source: "KAKAO_LOCAL", usage: ["schedule"] },
  { id: "place_3", planId: "plan_jeju", name: "흑돼지 맛집 연돈", address: "제주 안덕면 산방로 391", emoji: "🍽️", source: "KAKAO_LOCAL", usage: ["vote_candidate"] },
  { id: "place_4", planId: "plan_jeju", name: "해물탕 제주바다", address: "제주 시내", emoji: "🍲", source: "KAKAO_LOCAL", usage: ["vote_candidate"] },
  { id: "place_5", planId: "plan_jeju", name: "한라산 둘레길", address: "제주 한라산", emoji: "⛰️", source: "KAKAO_LOCAL", usage: ["schedule"] },
  { id: "place_6", planId: "plan_jeju", name: "오설록 티뮤지엄", address: "제주 서광서리", emoji: "🍵", source: "KAKAO_LOCAL", usage: ["schedule"] },
  { id: "place_7", planId: "plan_busan", name: "haeundae 해운대", address: "부산 해운대구", emoji: "🏖️", source: "KAKAO_LOCAL", usage: ["schedule"] },
  { id: "place_8", planId: "plan_busan", name: "센텀 코워킹카페", address: "부산 센텀시티", emoji: "☕", source: "KAKAO_LOCAL", usage: ["schedule"] },
];

export const INITIAL_SCHEDULES: Schedule[] = [
  // Day 1 · 8.14
  { id: "sch_1", planId: "plan_jeju", day: 1, date: `${YEAR}-08-14`, time: "10:30", placeName: "제주국제공항 도착", emoji: "✈️" },
  { id: "sch_2", planId: "plan_jeju", day: 1, date: `${YEAR}-08-14`, time: "12:00", placeName: "숙소 체크인", placeAddress: "씨원리조트", emoji: "🏨" },
  { id: "sch_3", planId: "plan_jeju", day: 1, date: `${YEAR}-08-14`, time: "19:00", placeName: "흑돼지 저녁", placeAddress: "제주 시내", emoji: "🍖", memo: "웰컴 디너" },
  // Day 2 · 8.15
  { id: "sch_4", planId: "plan_jeju", day: 2, date: `${YEAR}-08-15`, time: "09:00", placeName: "협재해변", placeAddress: "제주 한림읍 협재리", emoji: "📍", memo: "물놀이 + 사진 촬영, 파라솔 대여 필요" },
  { id: "sch_5", planId: "plan_jeju", day: 2, date: `${YEAR}-08-15`, time: "12:30", placeName: "카페 델문도", placeAddress: "협재 뷰 카페", emoji: "📍", memo: "협재 뷰 카페" },
  { id: "sch_6", planId: "plan_jeju", day: 2, date: `${YEAR}-08-15`, time: "19:00", placeName: "저녁 맛집 (투표중)", placeAddress: "3개 후보", emoji: "🗳️", linkedVoteId: "vote_1" },
  // Day 3 · 8.16
  { id: "sch_7", planId: "plan_jeju", day: 3, date: `${YEAR}-08-16`, time: "09:30", placeName: "한라산 둘레길", placeAddress: "제주 한라산", emoji: "⛰️", memo: "가벼운 트레킹 코스" },
  { id: "sch_8", planId: "plan_jeju", day: 3, date: `${YEAR}-08-16`, time: "14:00", placeName: "오설록 티뮤지엄", placeAddress: "제주 서광서리", emoji: "🍵" },
  // Day 4 · 8.17
  { id: "sch_9", planId: "plan_jeju", day: 4, date: `${YEAR}-08-17`, time: "10:00", placeName: "숙소 체크아웃", placeAddress: "씨원리조트", emoji: "🧳" },
  { id: "sch_10", planId: "plan_jeju", day: 4, date: `${YEAR}-08-17`, time: "13:00", placeName: "공항으로 이동", emoji: "✈️" },

  // 부산 워케이션
  { id: "sch_11", planId: "plan_busan", day: 1, date: `${YEAR}-10-03`, time: "11:00", placeName: "센텀 코워킹카페", placeAddress: "부산 센텀시티", emoji: "💻", memo: "오전 업무" },
  { id: "sch_12", planId: "plan_busan", day: 1, date: `${YEAR}-10-03`, time: "18:00", placeName: "해운대", placeAddress: "부산 해운대구", emoji: "🏖️" },
];

export const INITIAL_VOTES: Vote[] = [
  {
    id: "vote_1",
    planId: "plan_jeju",
    title: "둘째날 저녁 뭐 먹지?",
    status: "OPEN",
    deadline: hoursFromNow(3),
    linkedScheduleId: "sch_6",
    myOptionId: "vopt_1",
    options: [
      { id: "vopt_1", voteId: "vote_1", placeName: "흑돼지 맛집 '연돈'", placeAddress: "제주 안덕면 산방로 391", emoji: "🍽️", voteCount: 3 },
      { id: "vopt_2", voteId: "vote_1", placeName: "해물탕 '제주바다'", placeAddress: "제주 시내", emoji: "🍲", voteCount: 1 },
    ],
  },
  {
    id: "vote_2",
    planId: "plan_jeju",
    title: "숙소 어디로 할까?",
    status: "CLOSED",
    deadline: minutesAgo(60 * 24),
    resultSummary: "씨원리조트 3표 · 확정",
    options: [
      { id: "vopt_3", voteId: "vote_2", placeName: "씨원리조트", placeAddress: "제주 서귀포시", emoji: "🏨", voteCount: 3 },
      { id: "vopt_4", voteId: "vote_2", placeName: "게스트하우스 하늘", placeAddress: "제주 한림읍", emoji: "🛏️", voteCount: 1 },
    ],
  },
];

export const INITIAL_COMMENTS: Comment[] = [
  { id: "cmt_1", scheduleId: "sch_4", authorName: "김민수", authorColor: "#00C896", text: "파라솔 미리 예약해둘게요", createdAt: minutesAgo(50) },
  { id: "cmt_2", scheduleId: "sch_4", authorName: "박서연", authorColor: "#FF9F1C", text: "오전엔 사람 적대요, 좋아요", createdAt: minutesAgo(35) },
  { id: "cmt_3", scheduleId: "sch_4", authorName: "최도윤", authorColor: "#8B7FF2", text: "저도 오전 조로 참여할게요!", createdAt: minutesAgo(20) },
];

export const INITIAL_ACTIVITIES: ActivityLog[] = [
  { id: "act_1", planId: "plan_jeju", actorName: "김민수", actorColor: "#00C896", actorInitial: "김", type: "schedule_added", summary: "'협재해변' 일정을 추가했어요", targetType: "schedule", targetId: "sch_4", createdAt: minutesAgo(3) },
  { id: "act_2", planId: "plan_jeju", actorName: "박서연", actorColor: "#FF9F1C", actorInitial: "박", type: "vote_participated", summary: "투표에 참여했어요", targetType: "vote", targetId: "vote_1", createdAt: minutesAgo(10) },
  { id: "act_3", planId: "plan_jeju", actorName: "최도윤", actorColor: "#8B7FF2", actorInitial: "최", type: "member_joined", summary: "계획에 참여했어요", targetType: "member", targetId: "mem_4", createdAt: minutesAgo(60) },
  { id: "act_4", planId: "plan_jeju", actorName: "이지은", actorColor: "#3182F6", actorInitial: "이", type: "invitation_reissued", summary: "초대 링크를 재발급했어요", targetType: "member", createdAt: minutesAgo(60 * 26) },
  { id: "act_5", planId: "plan_jeju", actorName: "박서연", actorColor: "#FF9F1C", actorInitial: "박", type: "vote_created", summary: "'둘째날 저녁 뭐 먹지?' 투표를 시작했어요", targetType: "vote", targetId: "vote_1", createdAt: minutesAgo(60 * 48) },
  { id: "act_6", planId: "plan_jeju", actorName: "김민수", actorColor: "#00C896", actorInitial: "김", type: "schedule_added", summary: "'카페 델문도' 일정을 추가했어요", targetType: "schedule", targetId: "sch_5", createdAt: minutesAgo(60 * 49) },
  { id: "act_7", planId: "plan_busan", actorName: "이지은", actorColor: "#3182F6", actorInitial: "이", type: "schedule_added", summary: "'해운대' 일정을 추가했어요", targetType: "schedule", targetId: "sch_12", createdAt: minutesAgo(60 * 20) },
];
