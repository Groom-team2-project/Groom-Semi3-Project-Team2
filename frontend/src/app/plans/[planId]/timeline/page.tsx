"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Toast } from "@/components/ui/Toast";
import { DayTabs } from "@/components/plan/DayTabs";
import { TimelineStop } from "@/components/plan/TimelineStop";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { useRememberPlan } from "@/lib/lastPlan";
import { getSchedules, reorderSchedules } from "@/lib/api";
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
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [isReordering, setIsReordering] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const didDragRef = useRef(false);

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
  const daySchedules = schedules
    .filter((schedule) => schedule.day === activeDay)
    .sort((first, second) => first.time.localeCompare(second.time));
  const reorderableTimes = new Set(
    daySchedules
      .filter((schedule, index, items) => items.some((other, otherIndex) => otherIndex !== index && other.time === schedule.time))
      .map((schedule) => schedule.time),
  );

  async function moveSchedule(targetId: string) {
    if (!draggingId || draggingId === targetId || isReordering) return;

    const previous = schedules;
    const reorderedDay = [...daySchedules];
    const fromIndex = reorderedDay.findIndex((schedule) => schedule.id === draggingId);
    const toIndex = reorderedDay.findIndex((schedule) => schedule.id === targetId);
    if (fromIndex < 0 || toIndex < 0) return;
    if (reorderedDay[fromIndex].time !== reorderedDay[toIndex].time) return;

    const [moved] = reorderedDay.splice(fromIndex, 1);
    reorderedDay.splice(toIndex, 0, moved);

    let dayIndex = 0;
    const reorderedAll = schedules.map((schedule) =>
      schedule.day === activeDay ? reorderedDay[dayIndex++] : schedule,
    );

    setSchedules(reorderedAll);
    setDraggingId(null);
    setIsReordering(true);
    try {
      await reorderSchedules(planId, reorderedAll.map((schedule) => schedule.id));
      setToast("일정 순서를 변경했어요.");
    } catch {
      setSchedules(previous);
      setToast("순서를 저장하지 못했어요. 다시 시도해 주세요.");
    } finally {
      setIsReordering(false);
    }
  }

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
        {reorderableTimes.size > 0 && (
          <p className="px-1 text-[12px] text-gray-500">같은 시간에 시작하는 일정은 블록을 드래그해 순서를 바꿀 수 있어요.</p>
        )}
        {daySchedules.length === 0 && (
          <EmptyState emoji="🗓️" title="아직 등록된 일정이 없어요" description="+ 버튼을 눌러 첫 일정을 추가해보세요" />
        )}
        <div className="flex flex-col">
          {daySchedules.map((s) => (
            <div
              key={s.id}
              draggable={!isReordering && reorderableTimes.has(s.time)}
              onDragStart={(event) => {
                if (!reorderableTimes.has(s.time)) {
                  event.preventDefault();
                  return;
                }
                didDragRef.current = true;
                setDraggingId(s.id);
                event.dataTransfer.effectAllowed = "move";
                event.dataTransfer.setData("text/plain", s.id);
              }}
              onDragOver={(event) => {
                const draggingSchedule = daySchedules.find((schedule) => schedule.id === draggingId);
                if (!draggingSchedule || draggingSchedule.time !== s.time) return;
                event.preventDefault();
                event.dataTransfer.dropEffect = "move";
              }}
              onDrop={(event) => {
                event.preventDefault();
                void moveSchedule(s.id);
              }}
              onDragEnd={() => {
                setDraggingId(null);
                window.setTimeout(() => {
                  didDragRef.current = false;
                }, 0);
              }}
              className={draggingId && draggingId !== s.id ? "border-t-2 border-transparent hover:border-primary" : ""}
            >
              <TimelineStop
                schedule={s}
                isDragging={draggingId === s.id}
                isDraggable={reorderableTimes.has(s.time)}
                onSelect={() => {
                  if (didDragRef.current) return;
                  router.push(
                    s.linkedVoteId ? `/plans/${planId}/votes/${s.linkedVoteId}` : `/plans/${planId}/schedules/${s.id}`,
                  );
                }}
              />
            </div>
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
      <Toast message={toast} onClose={() => setToast(null)} />
    </div>
  );
}
