"use client";

import { use, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Toast } from "@/components/ui/Toast";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { getSavedPlaces, removePlaceFromPlan } from "@/lib/api";
import { ApiError } from "@/lib/api/client";
import type { Place } from "@/lib/api";
import { setPickedPlace } from "@/lib/pickedPlace";

function removePlaceErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "장소를 삭제하지 못했습니다.";
  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 403) return "이 계획에서 장소를 삭제할 권한이 없습니다.";
  if (error.status === 404) return "삭제할 장소나 계획을 찾을 수 없습니다.";
  if (error.status >= 500) return "서버에 문제가 있어 장소를 삭제하지 못했습니다.";
  return error.message || "장소를 삭제하지 못했습니다.";
}

function safeReturnPath(value: string | undefined, planId: string): string | null {
  if (!value) return null;
  try {
    const decoded = decodeURIComponent(value);
    return decoded.startsWith(`/plans/${planId}/`) ? decoded : null;
  } catch {
    return null;
  }
}

export default function PlaceListPage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ return?: string }>;
}) {
  const { planId } = use(params);
  const { return: returnQuery } = use(searchParams);
  const router = useRouter();
  const pickReturn = safeReturnPath(returnQuery, planId);
  const [places, setPlaces] = useState<Place[] | null>(null);
  const [forbidden, setForbidden] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const closeToast = useCallback(() => setToast(null), []);

  useEffect(() => {
    let cancelled = false;
    getSavedPlaces(planId)
      .then((saved) => {
        if (!cancelled) setPlaces(saved);
      })
      .catch((error) => {
        if (cancelled) return;
        if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
          setForbidden(true);
          return;
        }
        setPlaces([]);
      });
    return () => {
      cancelled = true;
    };
  }, [planId]);

  const searchReturn = encodeURIComponent(pickReturn ?? `/plans/${planId}/places`);

  function handleSelect(place: Place) {
    if (!pickReturn) return;
    setPickedPlace(place);
    router.push(pickReturn);
  }

  async function handleRemove(place: Place) {
    try {
      await removePlaceFromPlan(planId, place.id);
      setPlaces((current) => current?.filter((item) => item.id !== place.id) ?? []);
    } catch (error) {
      setToast(removePlaceErrorMessage(error));
    }
  }

  if (forbidden) return <PlanNotFound />;

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="저장된 장소" backHref={pickReturn ?? `/plans/${planId}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <p className="text-[12px] text-gray-500">
          {pickReturn
            ? "장소를 누르면 이전 화면으로 돌아갑니다"
            : `이 계획에서 검색해 저장한 장소 ${places?.length ?? 0}곳 · 일정과 투표에서 재사용됩니다`}
        </p>
        {places?.length === 0 && (
          <EmptyState emoji="📍" title="저장된 장소가 없어요" description="장소를 검색해서 일정과 투표에 사용해보세요" />
        )}
        <div>
          {places?.map((p) => (
            <PlaceRow
              key={p.id}
              emoji={p.emoji}
              name={p.name}
              address={p.address}
              onClick={pickReturn ? () => handleSelect(p) : undefined}
              onRemove={pickReturn ? undefined : () => void handleRemove(p)}
              tag={
                pickReturn
                  ? undefined
                  : p.usage.includes("vote_candidate")
                    ? { label: "투표 후보", color: "orange" }
                    : p.usage.includes("schedule")
                      ? { label: "일정", color: "blue" }
                      : undefined
              }
            />
          ))}
        </div>
        <div className="h-0.5" />
        <Button href={`/plans/${planId}/places/search?return=${searchReturn}`} variant="ghost" size="sm">
          🔍 새 장소 검색해서 추가
        </Button>
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
