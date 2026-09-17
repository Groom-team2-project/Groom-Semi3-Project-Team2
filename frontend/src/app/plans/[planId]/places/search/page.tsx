"use client";

import { use, useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { Segmented } from "@/components/ui/Segmented";
import { Toast } from "@/components/ui/Toast";
import { Button } from "@/components/ui/Button";
import { FieldInput } from "@/components/ui/FieldInput";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchMap, type PlaceSearchMapHandle } from "@/components/plan/PlaceSearchMap";
import { addPlaceToPlan, getSavedPlaces, searchMapPlaces } from "@/lib/api";
import { searchPlaceAddresses, type PlaceAddress } from "@/lib/api/placeAddresses";
import { ApiError } from "@/lib/api/client";
import type { MapPlaceQuery, PlaceSearchResult, PlaceUsage } from "@/lib/api";
import { setPickedPlace } from "@/lib/pickedPlace";

const CATEGORIES = [
  { value: "AT4", label: "관광" }, { value: "FD6", label: "음식점" },
  { value: "CE7", label: "카페" }, { value: "AD5", label: "숙소" },
];

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


export default function PlaceSearchPage({ params, searchParams }: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ return?: string; usage?: string }>;
}) {
  const { planId } = use(params);
  const { return: returnPath, usage } = use(searchParams);
  const router = useRouter();
  const destination = safeReturnPath(returnPath, planId);
  const map = useRef<PlaceSearchMapHandle>(null);
  const list = useRef<HTMLDivElement>(null);
  const requestId = useRef(0);
  const query = useRef<MapPlaceQuery>({});
  const [activeKeyword, setActiveKeyword] = useState("");
  const [input, setInput] = useState("");
  const [category, setCategory] = useState("");
  const [address, setAddress] = useState<string | null>(null);
  const [addresses, setAddresses] = useState<PlaceAddress[]>([]);
  const [results, setResults] = useState<PlaceSearchResult[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [savedIds, setSavedIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [total, setTotal] = useState(0);
  const [mapReady, setMapReady] = useState(false);
  const [moved, setMoved] = useState(false);
  const [addingId, setAddingId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const closeToast = useCallback(() => setToast(null), []);
  const onMapReady = useCallback(() => setMapReady(true), []);
  const onMapMove = useCallback(() => setMoved(true), []);

  useEffect(() => {
    let cancelled = false;
    getSavedPlaces(planId).then((saved) => {
      if (!cancelled) setSavedIds(new Set(saved.map((place) => place.kakaoId).filter((id): id is string => Boolean(id))));
    }).catch(() => {});
    return () => { cancelled = true; requestId.current += 1; };
  }, [planId]);

  const selectPlace = useCallback((id: string) => {
    setSelectedId(id);
    const row = list.current?.querySelector<HTMLElement>(`[data-place-id="${CSS.escape(id)}"]`);
    if (row && list.current) list.current.scrollTo({ top: row.offsetTop - list.current.offsetTop, behavior: "smooth" });
  }, []);

  async function runSearch(next: MapPlaceQuery, nextPage = 1, moveToFirst = false) {
    const id = ++requestId.current;
    query.current = next;
    setActiveKeyword(next.keyword ?? "");
    setSearchError(null);
    setAddresses([]);
    if (nextPage === 1) {
      setResults([]); setSelectedId(null); setPage(0); setHasNext(false); setTotal(0);
      setHasSearched(false); setMoved(false);
      list.current?.scrollTo({ top: 0 });
    }
    if (!next.keyword && !next.category) { setLoading(false); return; }
    setLoading(true);
    try {
      const response = await searchMapPlaces(next, nextPage);
      if (id !== requestId.current) return;
      setResults((previous) => nextPage === 1 ? response.places
        : [...new Map([...previous, ...response.places].map((place) => [place.kakaoId, place])).values()]);
      setTotal(response.totalCount); setHasNext(response.hasNext); setPage(nextPage); setHasSearched(true);
      if (nextPage === 1 && response.places.length) {
        const first = response.places[0];
        setSelectedId(first.kakaoId);
        if (moveToFirst && Number.isFinite(first.latitude) && Number.isFinite(first.longitude)) {
          map.current?.move({ latitude: first.latitude!, longitude: first.longitude! });
        }
      }
    } catch (error) {
      if (id === requestId.current) {
        setSearchError(searchErrorMessage(error));
        setToast(searchErrorMessage(error));
      }
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }

  async function chooseAddress(value: PlaceAddress, selectedCategory = category) {
    setAddress(value.address); setInput(value.address); setAddresses([]);
    const bounds = map.current?.move(value);
    await runSearch({ category: selectedCategory || undefined, bounds });
  }

  async function submitSearch() {
    const keyword = input.trim();
    if (!keyword) {
      setAddress(null);
      await runSearch({ category: category || undefined, bounds: map.current?.bounds() });
      return;
    }
    const id = ++requestId.current;
    setLoading(true); setSearchError(null); setHasNext(false); setAddresses([]);
    try {
      // 주소 검색의 성공 결과만 지도 이동 대상으로 사용한다. 장소명은 백엔드 키워드 검색으로 전달한다.
      const matches = mapReady ? await searchPlaceAddresses(keyword) : [];
      if (id !== requestId.current) return;
      if (matches.length === 1) { await chooseAddress(matches[0]); return; }
      if (matches.length > 1) {
        setResults([]); setTotal(0); setHasSearched(false); setSelectedId(null); setAddresses(matches);
        return;
      }
      setAddress(null);
      await runSearch({ keyword, category: category || undefined }, 1, true);
    } catch (error) {
      if (id === requestId.current) {
        setSearchError(error instanceof Error ? error.message : "검색하지 못했습니다.");
      }
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }

  function changeCategory(value: string) {
    const next = category === value ? "" : value;
    setCategory(next);
    const keyword = address ? undefined : query.current.keyword;
    void runSearch({ keyword, category: next || undefined, bounds: map.current?.bounds() });
  }

  function clearSearch() {
    setInput(""); setAddress(null); setAddresses([]);
    void runSearch({ category: category || undefined, bounds: map.current?.bounds() });
  }

  async function handleAdd(result: PlaceSearchResult) {
    if (addingId) return;
    setAddingId(result.kakaoId);
    try {
      const selectedUsage: PlaceUsage = usage === "schedule" || usage === "vote_candidate" ? usage : "saved";
      const place = await addPlaceToPlan(planId, result, selectedUsage);
      setSavedIds((previous) => new Set(previous).add(result.kakaoId));
      setPickedPlace(place);
      router.push(destination);
    } catch (error) {
      setToast(placeSelectionErrorMessage(error));
    } finally { setAddingId(null); }
  }

  return (
    <div className="flex h-dvh flex-col overflow-hidden bg-white">
      <AppBar title="장소 검색" backHref={destination} />
      <div className="shrink-0 px-4 pb-3">
        <form className="mb-3 flex items-center gap-2" onSubmit={(event) => {
          event.preventDefault();
          (document.activeElement as HTMLElement)?.blur();
          void submitSearch();
        }}>
          <div className="relative min-w-0 flex-1">
            <FieldInput value={input} onChange={(event) => setInput(event.target.value)}
              aria-label="장소, 주소로 검색" placeholder="장소, 주소로 검색" className="pr-10" />
            {input && <button type="button" aria-label="검색어 지우기" onClick={clearSearch}
              className="absolute right-2 top-1/2 -translate-y-1/2 p-2 text-gray-500">×</button>}
          </div>
          <Button type="submit" size="sm" fullWidth={false}>검색</Button>
        </form>
        <Segmented value={category} onChange={changeCategory} options={CATEGORIES} />
      </div>
      <div className="relative mx-4 h-[34dvh] min-h-40 max-h-80 shrink-0">
        <PlaceSearchMap ref={map} places={results} selectedId={selectedId} onSelect={selectPlace}
          onMove={onMapMove} onReady={onMapReady} />
        {mapReady && moved && (activeKeyword || category) && (
          <div className="absolute bottom-7 left-1/2 z-10 -translate-x-1/2 whitespace-nowrap">
            <Button size="sm" variant="ghost" disabled={loading}
              onClick={() => void runSearch({ ...query.current, bounds: map.current?.bounds() })}>이 지역에서 다시 검색</Button>
          </div>
        )}
      </div>
      <div className="flex shrink-0 items-center justify-between gap-2 px-4 py-3 text-xs text-gray-500" aria-live="polite">
        <span className="min-w-0 truncate">{address ? address : hasSearched ? `검색 결과 ${total}곳 · ${results.length}곳 표시` : "장소를 검색하거나 카테고리를 선택해 주세요"}</span>
        {loading && <span className="shrink-0 text-primary">검색 중…</span>}
      </div>
      <div ref={list} className="relative min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 pb-6" aria-label="장소 검색 결과" aria-busy={loading}>
        {addresses.length > 0 && <div className="flex flex-col gap-2">
          <p className="text-sm font-bold">이동할 주소를 선택해 주세요</p>
          {addresses.map((candidate) => <Button key={candidate.address} variant="ghost" size="sm"
            onClick={() => void chooseAddress(candidate)}>{candidate.address}</Button>)}
        </div>}
        {results.map((place) => (
          <div key={place.kakaoId} data-place-id={place.kakaoId}
            className={selectedId === place.kakaoId ? "rounded-xl bg-primary-soft px-2" : "px-2"}>
            <PlaceRow emoji={place.emoji} name={place.name}
              address={`${place.address}${place.category ? " · " + place.category : ""}`}
              onClick={() => {
                selectPlace(place.kakaoId);
                if (Number.isFinite(place.latitude) && Number.isFinite(place.longitude))
                  map.current?.move({ latitude: place.latitude!, longitude: place.longitude! });
              }}
              onAdd={() => void handleAdd(place)} addDisabled={addingId !== null || loading}
              tag={savedIds.has(place.kakaoId) ? { label: "저장됨", color: "gray" } : undefined} />
          </div>
        ))}
        {searchError && <div role="alert" className="my-3 rounded-xl bg-red-soft p-3 text-sm text-red">{searchError}</div>}
        {!loading && !searchError && !addresses.length && !results.length && <EmptyState
          emoji={hasSearched ? "🔍" : "🧭"}
          title={hasSearched ? "검색 결과가 없어요" : address ? "이 주소 주변을 둘러보세요" : "어디로 떠나볼까요?"}
          description={hasSearched ? "검색어나 카테고리를 바꾸거나 지도를 이동해 보세요." : "장소·주소를 검색하거나 지도 위 지역의 카테고리를 선택해 보세요."} />}
        {hasNext && <div className="pt-4"><Button variant="ghost" disabled={loading}
          onClick={() => void runSearch(query.current, page + 1)}>{loading ? "불러오는 중…" : "더 보기"}</Button></div>}
        {addingId && <p role="status" className="py-3 text-center text-sm text-gray-500">장소를 추가하는 중…</p>}
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
