"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { DayTabs } from "@/components/plan/DayTabs";
import { TimelineStop } from "@/components/plan/TimelineStop";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { useRememberPlan } from "@/lib/lastPlan";
import { getSchedules } from "@/lib/api";
import type { Schedule } from "@/lib/api";
import { dateRangeToDayCount, dayIndexToDate, formatDateShort } from "@/lib/utils";

export default function TimelinePage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ day?: string }>;
}) {
  const { planId } = use(params);
  const { day: dayParam } = use(searchParams);
  const router = useRouter();
  const { plan, isLoading } = usePlan(planId);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [activeDay, setActiveDay] = useState(Number(dayParam) || 1);
  const [syncedDayParam, setSyncedDayParam] = useState(dayParam);

  useRememberPlan(planId);

  useEffect(() => {
    getSchedules(planId).then(setSchedules);
  }, [planId]);

  // ?day= 쿼리로 새로 진입했을 때(예: 홈에서 다른 Day 카드 클릭) 탭 상태를 맞춰줌.
  // effect가 아니라 렌더 중 상태 조정 패턴 사용 (https://react.dev/learn/you-might-not-need-an-effect)
  if (dayParam !== syncedDayParam) {
    setSyncedDayParam(dayParam);
    setActiveDay(Number(dayParam) || 1);
  }

  if (isLoading) return null;
  if (!plan) return <PlanNotFound />;

  const dayCount = dateRangeToDayCount(plan.startDate, plan.endDate);
  const days = Array.from({ length: dayCount }, (_, i) => ({
    day: i + 1,
    dateLabel: formatDateShort(dayIndexToDate(plan.startDate, i + 1)).replace(/\(.+\)/, ""),
  }));
  const daySchedules = schedules.filter((s) => s.day === activeDay).sort((a, b) => a.time.localeCompare(b.time));

  return (
    <div className="relative flex min-h-dvh flex-col">
      <AppBar
        title="일정"
        backHref={`/plans/${planId}`}
        actions={
          <Button href={`/plans/${planId}/timeline/route?day=${activeDay}`} variant="ghost" size="sm" fullWidth={false}>
            동선 보기
          </Button>
        }
      />
      <div className="flex flex-1 flex-col gap-3.5 px-4 pb-24">
        <DayTabs days={days} active={activeDay} onChange={setActiveDay} />
        {daySchedules.length === 0 && (
          <EmptyState emoji="🗓️" title="아직 등록된 일정이 없어요" description="+ 버튼을 눌러 첫 일정을 추가해보세요" />
        )}
        <div className="flex flex-col">
          {daySchedules.map((s) => (
            <TimelineStop
              key={s.id}
              schedule={s}
              onClick={() =>
                router.push(
                  s.linkedVoteId ? `/plans/${planId}/votes/${s.linkedVoteId}` : `/plans/${planId}/schedules/${s.id}`,
                )
              }
            />
          ))}
        </div>
      </div>
      <button
        type="button"
        onClick={() => router.push(`/plans/${planId}/schedules/new?day=${activeDay}`)}
        className="fixed bottom-24 right-4 z-20 flex items-center gap-1.5 rounded-full bg-primary px-4.5 py-3 text-[13.5px] font-bold text-white shadow-lg sm:right-[calc(50%-220px)]"
      >
        ＋ 일정 추가
      </button>
    </div>
  );
}
