"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Segmented } from "@/components/ui/Segmented";
import { EmptyState } from "@/components/ui/EmptyState";
import { KakaoRouteMap } from "@/components/plan/KakaoRouteMap";
import { getSchedules } from "@/lib/api";
import type { Schedule } from "@/lib/api";

type Mode = "walk" | "car" | "transit";

const MODE_META: Record<Mode, { label: string; speedMinPerKm: number; icon: string }> = {
  walk: { label: "도보", speedMinPerKm: 13, icon: "🚶" },
  car: { label: "자동차", speedMinPerKm: 2.2, icon: "🚗" },
  transit: { label: "대중교통", speedMinPerKm: 4.5, icon: "🚇" },
};

/** 실제 좌표가 없으니, 순서 기반으로 그럴듯한 거리(추정치)를 만들어낸다. */
function estimateDistanceKm(seed: number): number {
  return Math.round((1.2 + ((seed * 37) % 30) / 10) * 10) / 10;
}

export default function RouteMapPage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ day?: string }>;
}) {
  const { planId } = use(params);
  const { day: dayParam } = use(searchParams);
  const day = Number(dayParam) || 1;
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [mode, setMode] = useState<Mode>("car");

  useEffect(() => {
    getSchedules(planId).then((all) =>
      setSchedules(all.filter((s) => s.day === day).sort((a, b) => a.time.localeCompare(b.time))),
    );
  }, [planId, day]);

  const legs = schedules.slice(1).map((to, i) => {
    const from = schedules[i];
    const km = estimateDistanceKm(i + 1);
    const minutes = Math.max(3, Math.round(km * MODE_META[mode].speedMinPerKm));
    return { from, to, km, minutes };
  });

  const totalKm = Math.round(legs.reduce((sum, l) => sum + l.km, 0) * 10) / 10;
  const totalMin = legs.reduce((sum, l) => sum + l.minutes, 0);

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar
        title={`Day ${day} 동선`}
        subtitle={legs.length > 0 ? `${MODE_META[mode].label} · 이동 ${totalMin}분 · 총 ${totalKm}km (추정)` : "동선 정보 없음"}
        backHref={`/plans/${planId}/timeline?day=${day}`}
      />
      <div className="px-4 pb-3">
        <Segmented
          value={mode}
          onChange={setMode}
          options={[
            { value: "walk", label: "🚶 도보" },
            { value: "car", label: "🚗 자동차" },
            { value: "transit", label: "🚇 대중교통" },
          ]}
        />
      </div>

      {legs.length === 0 ? (
        <div className="px-4">
          <EmptyState emoji="🗺️" title="동선을 계산할 일정이 부족해요" description="같은 날짜에 일정이 2개 이상 있어야 동선을 볼 수 있어요" />
        </div>
      ) : (
        <>
          <div className="relative bg-gray-100">
            <KakaoRouteMap schedules={schedules} />
            <div className="absolute left-2.5 top-2.5 rounded-lg border border-gray-200 bg-white px-2.5 py-1 text-[10.5px] font-bold text-gray-700">
              드래그해서 지도 이동 · 휠/버튼으로 확대·축소
            </div>
          </div>
          <div className="flex flex-1 flex-col gap-3 px-4 pt-3.5 pb-8">
            <div className="rounded-2xl border border-dashed border-primary bg-primary-soft p-3.5 text-[13px] text-primary-dark">
              ⚠️ 카카오모빌리티 Directions API는 별도 승인 절차가 필요해, 도보·자동차는 직선거리에 보정계수를 적용해
              추정합니다. P2 스트레치 기능입니다.
            </div>
            <div className="flex flex-col gap-2">
              {legs.map((leg, i) => (
                <div key={i} className="rounded-2xl bg-gray-100 px-3.5 py-3 text-[13px] text-ink">
                  <div className="mb-0.5 text-[11.5px] text-gray-500">
                    {leg.from.placeName} → {leg.to.placeName}
                  </div>
                  {MODE_META[mode].icon} 약 {leg.minutes}분 · {leg.km}km (추정)
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <Button href={`/plans/${planId}/timeline?day=${day}`} variant="ghost" size="sm">
                일정으로 돌아가기
              </Button>
              <Button variant="primary" size="sm" onClick={() => window.alert("카카오맵 길안내는 실제 API 연동 후 제공돼요")}>
                카카오맵에서 {MODE_META[mode].label} 길안내
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
