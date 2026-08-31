"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { getSavedPlaces } from "@/lib/api";
import { ApiError } from "@/lib/api/client";
import type { Place } from "@/lib/api";

export default function PlaceListPage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const [places, setPlaces] = useState<Place[] | null>(null);
  const [forbidden, setForbidden] = useState(false);

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

  const returnPath = encodeURIComponent(`/plans/${planId}/places`);

  if (forbidden) return <PlanNotFound />;

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="저장된 장소" backHref={`/plans/${planId}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <p className="text-[12px] text-gray-500">
          이 계획에서 검색해 저장한 장소 {places?.length ?? 0}곳 · 일정과 투표에서 재사용됩니다
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
              tag={
                p.usage.includes("vote_candidate")
                  ? { label: "투표 후보", color: "orange" }
                  : p.usage.includes("schedule")
                    ? { label: "일정", color: "blue" }
                    : { label: "저장됨", color: "gray" }
              }
            />
          ))}
        </div>
        <div className="h-0.5" />
        <Button href={`/plans/${planId}/places/search?return=${returnPath}`} variant="ghost" size="sm">
          🔍 새 장소 검색해서 추가
        </Button>
      </div>
    </div>
  );
}
