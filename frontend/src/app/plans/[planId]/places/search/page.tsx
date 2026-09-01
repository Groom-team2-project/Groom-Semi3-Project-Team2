"use client";

import { use, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { Segmented } from "@/components/ui/Segmented";
import { Toast } from "@/components/ui/Toast";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { addPlaceToPlan, getSavedPlaces, searchPlacesByKeyword, searchPlacesNearby, NEARBY_CATEGORIES } from "@/lib/api";
import { ApiError, USE_MOCK } from "@/lib/api/client";
import type { PlaceSearchLocation, PlaceSearchResult, PlaceUsage } from "@/lib/api";
import { setPickedPlace } from "@/lib/pickedPlace";

type Mode = "keyword" | "nearby";

function safeReturnPath(value: string | undefined, planId: string): string {
  const fallback = `/plans/${planId}/places`;
  if (!value) return fallback;

  try {
    const decoded = decodeURIComponent(value);
    return decoded.startsWith(`/plans/${planId}/`) ? decoded : fallback;
  } catch {
    return fallback;
  }
}

function searchErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "장소 검색 중 문제가 발생했습니다.";
  if (error.status === 401) return "장소를 검색하려면 로그인이 필요합니다.";
  if (error.status === 400) return error.message;
  if (error.status === 502) return "카카오 장소 검색 서버에 연결하지 못했습니다.";
  if (error.status === 0) return error.message;
  if (error.status >= 500) return "장소 검색 서버에 연결하지 못했습니다.";
  return "장소 검색 중 문제가 발생했습니다.";
}

function placeSelectionErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "장소 선택을 저장하지 못했습니다.";
  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 400) return error.message;
  if (error.status === 401) return "장소를 선택하려면 로그인이 필요합니다.";
  if (error.status === 403) return "이 계획에 장소를 추가할 권한이 없습니다.";
  if (error.status === 404) return "선택한 장소나 계획을 찾을 수 없습니다.";
  if (error.status >= 500) return "서버에 문제가 있어 장소를 저장하지 못했습니다.";
  return "장소 선택을 저장하지 못했습니다.";
}

function getCurrentLocation(): Promise<PlaceSearchLocation> {
  return new Promise((resolve, reject) => {
    if (!("geolocation" in navigator)) {
      reject(new ApiError("이 브라우저에서는 현재 위치를 사용할 수 없습니다.", 0, "LOCATION_UNAVAILABLE"));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude }),
      () => reject(new ApiError("현재 위치 권한을 확인해주세요.", 0, "LOCATION_PERMISSION_DENIED")),
      { enableHighAccuracy: false, timeout: 8_000, maximumAge: 300_000 },
    );
  });
}

export default function PlaceSearchPage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ return?: string; usage?: string }>;
}) {
  const { planId } = use(params);
  const { return: returnPath, usage } = use(searchParams);
  const router = useRouter();

  const [mode, setMode] = useState<Mode>("keyword");
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("all");
  const [results, setResults] = useState<PlaceSearchResult[]>([]);
  const [addingId, setAddingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [location, setLocation] = useState<PlaceSearchLocation | null>(null);
  const [savedKakaoIds, setSavedKakaoIds] = useState<Set<string>>(new Set());
  const destination = safeReturnPath(returnPath, planId);
  const closeToast = useCallback(() => setToast(null), []);

  useEffect(() => {
    let cancelled = false;
    getSavedPlaces(planId)
      .then((saved) => {
        if (cancelled) return;
        setSavedKakaoIds(new Set(saved.map((place) => place.kakaoId).filter((id): id is string => Boolean(id))));
      })
      .catch(() => {
        if (!cancelled) setSavedKakaoIds(new Set());
      });
    return () => {
      cancelled = true;
    };
  }, [planId]);

  useEffect(() => {
    if (mode !== "keyword") return;
    const normalizedKeyword = keyword.trim();
    if (!normalizedKeyword) return;

    let cancelled = false;
    const timeout = window.setTimeout(async () => {
      setLoading(true);
      try {
        const places = await searchPlacesByKeyword(normalizedKeyword);
        if (!cancelled) {
          setResults(places);
          setHasSearched(true);
        }
      } catch (error) {
        if (!cancelled) {
          setResults([]);
          setHasSearched(false);
          setToast(searchErrorMessage(error));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, 350);

    return () => {
      cancelled = true;
      window.clearTimeout(timeout);
    };
  }, [mode, keyword]);

  useEffect(() => {
    if (mode !== "nearby") return;
    if (USE_MOCK || location) return;
    let cancelled = false;

    async function loadCurrentLocation() {
      setLoading(true);
      try {
        const currentLocation = await getCurrentLocation();
        if (!cancelled) setLocation(currentLocation);
      } catch (error) {
        if (!cancelled) {
          setResults([]);
          setHasSearched(false);
          setToast(searchErrorMessage(error));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadCurrentLocation();
    return () => {
      cancelled = true;
    };
  }, [mode, location]);

  useEffect(() => {
    if (mode !== "nearby") return;
    if (!USE_MOCK && !location) return;
    let cancelled = false;

    async function loadNearbyPlaces() {
      setLoading(true);
      try {
        const places = await searchPlacesNearby(category, location ?? undefined);
        if (!cancelled) {
          setResults(places);
          setHasSearched(true);
        }
      } catch (error) {
        if (!cancelled) {
          setResults([]);
          setHasSearched(false);
          setToast(searchErrorMessage(error));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadNearbyPlaces();
    return () => {
      cancelled = true;
    };
  }, [mode, category, location]);

  function handleModeChange(nextMode: Mode) {
    setMode(nextMode);
    setResults([]);
    setHasSearched(false);
    setLoading(false);
  }

  function handleKeywordChange(value: string) {
    setKeyword(value);
    if (!value.trim()) {
      setResults([]);
      setHasSearched(false);
      setLoading(false);
    }
  }

  async function handleAdd(result: PlaceSearchResult) {
    setAddingId(result.kakaoId);
    try {
      const place = await addPlaceToPlan(planId, result, (usage as PlaceUsage) ?? "saved");
      if (result.kakaoId) {
        setSavedKakaoIds((current) => new Set(current).add(result.kakaoId));
      }
      setPickedPlace(place);
      router.push(destination);
    } catch (error) {
      setToast(placeSelectionErrorMessage(error));
    } finally {
      setAddingId(null);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="장소 검색" backHref={destination} />
      <div className="flex flex-1 flex-col gap-2.5 px-4 pb-8">
        <Segmented
          value={mode}
          onChange={handleModeChange}
          options={[
            { value: "keyword", label: "🔍 키워드 검색" },
            { value: "nearby", label: "📍 내 주변" },
          ]}
        />

        {mode === "keyword" ? (
          <div className="flex flex-col gap-2.5">
            <div className="flex items-center gap-2 rounded-full border border-gray-200 bg-gray-100 px-3.5 py-2.5 text-[14px] text-gray-700">
              <span>🔍</span>
              <input
                autoFocus
                value={keyword}
                onChange={(e) => handleKeywordChange(e.target.value)}
                placeholder="장소, 주소로 검색"
                className="flex-1 bg-transparent outline-none placeholder:text-gray-500"
              />
              <span className="shrink-0 rounded-md bg-orange-soft px-1.5 py-0.5 text-[10px] font-bold text-orange">
                KAKAO LOCAL API
              </span>
            </div>
            <div>
              {results.map((r) => (
                <PlaceRow
                  key={r.kakaoId}
                  emoji={r.emoji}
                  name={r.name}
                  address={`${r.address}${r.category ? ` · ${r.category}` : ""}`}
                  onAdd={() => handleAdd(r)}
                  addDisabled={addingId !== null}
                  tag={
                    savedKakaoIds.has(r.kakaoId)
                      ? { label: "저장됨", color: "gray" }
                      : undefined
                  }
                />
              ))}
            </div>
            {!loading && hasSearched && results.length === 0 && (
              <EmptyState emoji="🔍" title="검색 결과가 없어요" description="다른 장소명이나 주소로 검색해보세요" />
            )}
            <p className="pt-1.5 text-center text-[11.5px] text-gray-500">
              {loading ? "검색하는 중..." : addingId ? "추가하는 중..." : "장소명·주소는 카카오 로컬 API 키워드 검색 결과입니다"}
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-2.5">
            <div className="flex items-center justify-between rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14px] text-gray-700">
              <span>📍 {location ? "현재 위치 기준 2km 이내" : "현재 위치 확인 중"}</span>
              <span className="shrink-0 rounded-md bg-orange-soft px-1.5 py-0.5 text-[10px] font-bold text-orange">GPS</span>
            </div>
            <Segmented
              value={category}
              onChange={setCategory}
              options={NEARBY_CATEGORIES.map((c) => ({ value: c.code, label: c.label }))}
            />
            <div>
              {results.map((r) => (
                <PlaceRow
                  key={r.kakaoId}
                  emoji={r.emoji}
                  name={r.name}
                  address={`${r.distanceMeters !== undefined ? `${r.distanceMeters}m · ` : ""}${r.address}`}
                  onAdd={() => handleAdd(r)}
                  addDisabled={addingId !== null}
                  tag={
                    savedKakaoIds.has(r.kakaoId)
                      ? { label: "저장됨", color: "gray" }
                      : undefined
                  }
                />
              ))}
            </div>
            {!loading && hasSearched && results.length === 0 && (
              <EmptyState emoji="📍" title="주변 장소가 없어요" description="다른 카테고리를 선택해보세요" />
            )}
            <p className="pt-1.5 text-center text-[11.5px] text-gray-500">
              {loading ? "주변 장소를 찾는 중..." : "현재 좌표 기준 카카오 로컬 API 카테고리 검색 결과입니다"}
            </p>
          </div>
        )}
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
