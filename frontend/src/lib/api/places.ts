import { generateId } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import { apiFetch, ApiError, USE_MOCK } from "./client";
import type { Place, PlaceSearchResult, PlaceUsage } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}

interface PlaceDocumentApiResponse {
  kakaoPlaceId: string;
  name: string;
  category: string | null;
  categoryGroupCode: string | null;
  categoryGroupName: string | null;
  address: string | null;
  roadAddress: string | null;
  longitude: number | null;
  latitude: number | null;
  phone: string | null;
  placeUrl: string | null;
  selectionToken: string;
}

interface PlaceDocumentListApiResponse {
  places: PlaceDocumentApiResponse[];
  totalCount: number;
  pageableCount: number;
  isEnd: boolean;
}

interface PlaceApiResponse {
  placeId: number;
  name: string;
  category: string | null;
  address: string | null;
  roadAddress: string | null;
}

export interface PlaceSearchLocation {
  latitude: number;
  longitude: number;
}

const NEARBY_RADIUS_METERS = 2_000;

const CATEGORY_EMOJI: Record<string, string> = {
  AT4: "🏖️",
  AD5: "🏨",
  FD6: "🍽️",
  CE7: "☕",
};

function mapPlaceDocument(place: PlaceDocumentApiResponse): PlaceSearchResult {
  return {
    kakaoId: place.kakaoPlaceId,
    name: place.name,
    address: place.roadAddress || place.address || "주소 정보 없음",
    landLotAddress: place.address ?? undefined,
    roadAddress: place.roadAddress ?? undefined,
    category: place.category ?? undefined,
    categoryGroupCode: place.categoryGroupCode ?? undefined,
    categoryGroupName: place.categoryGroupName ?? undefined,
    longitude: place.longitude ?? undefined,
    latitude: place.latitude ?? undefined,
    phone: place.phone ?? undefined,
    placeUrl: place.placeUrl ?? undefined,
    selectionToken: place.selectionToken,
    emoji: CATEGORY_EMOJI[place.categoryGroupCode ?? ""] ?? "📍",
  };
}

function placeEmoji(category: string | null | undefined, categoryGroupCode?: string | null): string {
  if (categoryGroupCode && CATEGORY_EMOJI[categoryGroupCode]) {
    return CATEGORY_EMOJI[categoryGroupCode];
  }
  if (category?.includes("카페")) return "☕";
  if (category?.includes("음식점")) return "🍽️";
  if (category?.includes("숙박")) return "🏨";
  if (category?.includes("관광")) return "🏖️";
  return "📍";
}

function mapSavedPlace(planId: string, place: PlaceApiResponse): Place {
  return {
    id: String(place.placeId),
    planId,
    name: place.name,
    address: place.roadAddress || place.address || "주소 정보 없음",
    emoji: placeEmoji(place.category),
    category: place.category ?? undefined,
    source: "KAKAO_LOCAL",
    usage: ["saved"],
  };
}

function distanceInMeters(origin: PlaceSearchLocation, place: PlaceSearchResult): number | undefined {
  if (place.latitude === undefined || place.longitude === undefined) return undefined;

  const toRadians = (degree: number) => degree * (Math.PI / 180);
  const earthRadius = 6_371_000;
  const latitudeDelta = toRadians(place.latitude - origin.latitude);
  const longitudeDelta = toRadians(place.longitude - origin.longitude);
  const a = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(toRadians(origin.latitude))
    * Math.cos(toRadians(place.latitude))
    * Math.sin(longitudeDelta / 2) ** 2;

  return Math.round(earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

async function searchCategory(
  category: string,
  location: PlaceSearchLocation,
): Promise<PlaceSearchResult[]> {
  const latitudeDelta = NEARBY_RADIUS_METERS / 111_320;
  const longitudeScale = Math.max(Math.cos(location.latitude * (Math.PI / 180)), 0.01);
  const longitudeDelta = NEARBY_RADIUS_METERS / (111_320 * longitudeScale);
  const params = new URLSearchParams({
    southWestLongitude: String(location.longitude - longitudeDelta),
    southWestLatitude: String(location.latitude - latitudeDelta),
    northEastLongitude: String(location.longitude + longitudeDelta),
    northEastLatitude: String(location.latitude + latitudeDelta),
    page: "1",
    size: "15",
  });
  const response = await apiFetch<CommonResponse<PlaceDocumentListApiResponse>>(
    `/api/v1/place2/category/${encodeURIComponent(category)}?${params.toString()}`,
  );

  return response.data.places.map(mapPlaceDocument);
}

/** GET /api/v1/plans/{planId}/places — 이 계획에서 저장한 장소 */
export async function getSavedPlaces(planId: string): Promise<Place[]> {
  if (!USE_MOCK) {
    const response = await apiFetch<CommonResponse<PlaceApiResponse[]>>(
      `/api/v1/plans/${planId}/places`,
    );
    return response.data.map((place) => mapSavedPlace(planId, place));
  }
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
  const normalizedKeyword = keyword.trim();
  if (!normalizedKeyword) return [];

  if (!USE_MOCK) {
    const params = new URLSearchParams({ keyword: normalizedKeyword, page: "1", size: "15" });
    const response = await apiFetch<CommonResponse<PlaceDocumentListApiResponse>>(
      `/api/v1/place2/search?${params.toString()}`,
    );
    return response.data.places.map(mapPlaceDocument);
  }

  await simulateLatency(320);
  const q = normalizedKeyword.toLowerCase();
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
export async function searchPlacesNearby(
  category = "all",
  location?: PlaceSearchLocation,
): Promise<PlaceSearchResult[]> {
  if (!USE_MOCK) {
    if (!location) {
      throw new ApiError("현재 위치를 확인할 수 없습니다.", 0, "LOCATION_UNAVAILABLE");
    }

    const categories = category === "all"
      ? NEARBY_CATEGORIES.filter((item) => item.code !== "all").map((item) => item.code)
      : [category];
    const responses = await Promise.all(categories.map((code) => searchCategory(code, location)));
    const uniquePlaces = new Map<string, PlaceSearchResult>();

    for (const place of responses.flat()) {
      if (!uniquePlaces.has(place.kakaoId)) {
        uniquePlaces.set(place.kakaoId, {
          ...place,
          distanceMeters: distanceInMeters(location, place),
        });
      }
    }

    return [...uniquePlaces.values()].sort(
      (left, right) => (left.distanceMeters ?? Number.MAX_SAFE_INTEGER) - (right.distanceMeters ?? Number.MAX_SAFE_INTEGER),
    );
  }

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
  if (!USE_MOCK) {
    if (!result.selectionToken) {
      throw new ApiError(
        "장소 선택 정보가 없어 저장할 수 없습니다. 다시 검색해 주세요.",
        400,
        "INVALID_PLACE_SELECTION_TOKEN",
      );
    }

    const response = await apiFetch<CommonResponse<PlaceApiResponse>>(
      `/api/v1/plans/${planId}/places`,
      {
        method: "POST",
        body: JSON.stringify({
          selectionToken: result.selectionToken,
        }),
      },
    );

    return {
      ...mapSavedPlace(planId, response.data),
      usage: [usage],
    };
  }

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
