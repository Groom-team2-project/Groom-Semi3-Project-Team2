import { generateId } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import type { Place, PlaceSearchResult, PlaceUsage } from "./types";

/** GET /api/v1/plans/{planId}/places — 이 계획에서 저장한 장소 (일정/투표에서 재사용) */
export async function getSavedPlaces(planId: string): Promise<Place[]> {
  await simulateLatency(150);
  return store.places.filter((p) => p.planId === planId);
}

// 카카오 로컬 API 키워드 검색 mock 풀
const KEYWORD_POOL: Omit<PlaceSearchResult, "kakaoId">[] = [
  { name: "연돈", address: "제주 안덕면 산방로 391", emoji: "🍽️", category: "흑돼지 전문" },
  { name: "돈사돈", address: "제주 노형동 도령로 20", emoji: "🍽️", category: "웨이팅 30분" },
  { name: "몸국식당", address: "제주 한림읍 협재리", emoji: "🍽️", category: "로컬 맛집" },
  { name: "협재해변", address: "제주 한림읍 협재리", emoji: "🏖️", category: "해변" },
  { name: "카페 델문도", address: "제주 한림읍 협재리", emoji: "☕", category: "뷰 카페" },
  { name: "오설록 티뮤지엄", address: "제주 서광서리", emoji: "🍵", category: "체험 명소" },
  { name: "씨원리조트", address: "제주 서귀포시", emoji: "🏨", category: "숙소" },
];

/** GET /api/v1/places/search?query= — 카카오 로컬 API 키워드 검색 */
export async function searchPlacesByKeyword(keyword: string): Promise<PlaceSearchResult[]> {
  await simulateLatency(320);
  const q = keyword.trim().toLowerCase();
  const matched = q
    ? KEYWORD_POOL.filter(
        (p) => p.name.toLowerCase().includes(q) || p.address.toLowerCase().includes(q) || p.category?.toLowerCase().includes(q),
      )
    : KEYWORD_POOL;
  const results = matched.length > 0 ? matched : KEYWORD_POOL;
  return results.map((p) => ({ ...p, kakaoId: generateId("kko") }));
}

// 카카오 로컬 API 카테고리(내 주변) 검색 mock 풀
const NEARBY_POOL: (Omit<PlaceSearchResult, "kakaoId"> & { category: string })[] = [
  { name: "협재 게스트하우스", address: "숙소", emoji: "🛏️", category: "AD5", distanceMeters: 180 },
  { name: "베이글칸", address: "카페", emoji: "🥐", category: "CE7", distanceMeters: 230 },
  { name: "협재수우동", address: "맛집", emoji: "🍽️", category: "FD6", distanceMeters: 410 },
  { name: "협재 서핑스쿨", address: "관광", emoji: "🏄", category: "AT4", distanceMeters: 650 },
  { name: "봄날카페", address: "카페", emoji: "☕", category: "CE7", distanceMeters: 900 },
];

export const NEARBY_CATEGORIES = [
  { code: "all", label: "전체" },
  { code: "AT4", label: "관광" },
  { code: "FD6", label: "맛집" },
  { code: "CE7", label: "카페" },
  { code: "AD5", label: "숙소" },
] as const;

/** GET /api/v1/places/search?category=&lat=&lng= — 내 주변 카테고리 검색 */
export async function searchPlacesNearby(category = "all"): Promise<PlaceSearchResult[]> {
  await simulateLatency(280);
  const filtered = category === "all" ? NEARBY_POOL : NEARBY_POOL.filter((p) => p.category === category);
  return filtered.map((p) => ({ ...p, kakaoId: generateId("kko") }));
}

/** 검색 결과를 계획에 저장 (findOrCreateByKakaoId 패턴) */
export async function addPlaceToPlan(
  planId: string,
  result: PlaceSearchResult,
  usage: PlaceUsage = "saved",
): Promise<Place> {
  await simulateLatency(200);
  const existing = store.places.find((p) => p.planId === planId && p.name === result.name);
  if (existing) {
    if (!existing.usage.includes(usage)) existing.usage.push(usage);
    return existing;
  }
  const place: Place = {
    id: generateId("place"),
    planId,
    name: result.name,
    address: result.address,
    emoji: result.emoji,
    category: result.category,
    source: "KAKAO_LOCAL",
    usage: [usage],
  };
  store.places.push(place);
  return place;
}
