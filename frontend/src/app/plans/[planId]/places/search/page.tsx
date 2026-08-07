"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Segmented } from "@/components/ui/Segmented";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { addPlaceToPlan, searchPlacesByKeyword, searchPlacesNearby, NEARBY_CATEGORIES } from "@/lib/api";
import type { PlaceSearchResult, PlaceUsage } from "@/lib/api";
import { setPickedPlace } from "@/lib/pickedPlace";

type Mode = "keyword" | "nearby";

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

  useEffect(() => {
    if (mode === "keyword") {
      searchPlacesByKeyword(keyword).then(setResults);
    } else {
      searchPlacesNearby(category).then(setResults);
    }
  }, [mode, keyword, category]);

  async function handleAdd(result: PlaceSearchResult) {
    setAddingId(result.kakaoId);
    try {
      const place = await addPlaceToPlan(planId, result, (usage as PlaceUsage) ?? "saved");
      setPickedPlace(place);
      router.push(returnPath ? decodeURIComponent(returnPath) : `/plans/${planId}/places`);
    } finally {
      setAddingId(null);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="장소 검색" backHref={returnPath ? decodeURIComponent(returnPath) : `/plans/${planId}/places`} />
      <div className="flex flex-1 flex-col gap-2.5 px-4 pb-8">
        <Segmented
          value={mode}
          onChange={setMode}
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
                onChange={(e) => setKeyword(e.target.value)}
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
                />
              ))}
            </div>
            <p className="pt-1.5 text-center text-[11.5px] text-gray-500">
              {addingId ? "추가하는 중..." : "장소명·주소는 카카오 로컬 API 키워드 검색 결과입니다"}
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-2.5">
            <div className="flex items-center justify-between rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14px] text-gray-700">
              <span>📍 현재 위치: 여행지 인근</span>
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
                  address={`${r.distanceMeters}m · ${r.address}`}
                  onAdd={() => handleAdd(r)}
                />
              ))}
            </div>
            <p className="pt-1.5 text-center text-[11.5px] text-gray-500">
              현재 좌표 기준 카카오 로컬 API 카테고리 검색 결과입니다
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
