"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { BottomTabBar } from "@/components/ui/BottomTabBar";
import { ActivityRow } from "@/components/plan/ActivityRow";
import { Toast } from "@/components/ui/Toast";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { useRememberPlan } from "@/lib/lastPlan";
import { getActivities, getSchedules, getVotes } from "@/lib/api";
import { getActivityDestination } from "@/lib/activityNavigation";
import type { ActivityLog, Schedule, Vote } from "@/lib/api";
import { dateRangeToDayCount, dayIndexToDate, formatDateShort, formatDday, formatDeadline } from "@/lib/utils";

export default function PlanHomePage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const router = useRouter();
  const { plan, isLoading } = usePlan(planId);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [votes, setVotes] = useState<Vote[]>([]);
  const [activities, setActivities] = useState<ActivityLog[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useRememberPlan(planId);

  useEffect(() => {
    getSchedules(planId).then(setSchedules);
    getVotes(planId).then(setVotes);
    getActivities(planId, { size: 3 }).then((page) => setActivities(page.activities));
  }, [planId]);

  const clearToast = useCallback(() => setToastMessage(null), []);

  async function handleActivityClick(activity: ActivityLog) {
    const destination = await getActivityDestination(planId, activity);
    if (destination) {
      router.push(destination);
      return;
    }
    setToastMessage("삭제되었거나 더 이상 볼 수 없는 활동이에요.");
  }

  if (isLoading) return null;
  if (!plan) return <PlanNotFound />;

  const dayCount = dateRangeToDayCount(plan.startDate, plan.endDate);
  const days = Array.from({ length: dayCount }, (_, i) => i + 1);
  const openVote = votes.find((v) => v.status === "OPEN");
  const dday = formatDday(plan.startDate);

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar
        title={plan.title}
        subtitle={`${formatDateShort(plan.startDate)} - ${formatDateShort(plan.endDate)}${dday ? ` · ${dday}` : ""}`}
        actions={
          <>
            {(plan.myRole === "OWNER" || plan.myRole === "EDITOR") && (
                <Button href={`/plans/${planId}/edit`} variant="ghost" size="sm" fullWidth={false}>
                  수정
                </Button>
            )}
            <Button href={`/plans/${planId}/places`} variant="ghost" size="sm" fullWidth={false}>
              장소
            </Button>
            <Button href={`/plans/${planId}/members`} variant="ghost" size="sm" fullWidth={false}>
              멤버
            </Button>
          </>
        }
      />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        {openVote && (
          <button
            type="button"
            onClick={() => router.push(`/plans/${planId}/votes/${openVote.id}`)}
            className="flex w-full items-center gap-3 rounded-2xl bg-orange-soft p-3.5 text-left"
          >
            <span className="h-2 w-2 shrink-0 rounded-full bg-orange" />
            <span>
              <span className="block text-[14px] font-bold text-ink">🗳️ 마감 임박 투표 1건</span>
              <span className="block text-[12px] text-gray-700">
                &quot;{openVote.title}&quot; · {formatDeadline(openVote.deadline)}
              </span>
            </span>
          </button>
        )}

        <h3 className="mt-1 text-[15px] font-bold">날짜별 일정</h3>
        {days.map((day) => (
          <DayCard key={day} planId={planId} day={day} date={dayIndexToDate(plan.startDate, day)} schedules={schedules.filter((s) => s.day === day)} />
        ))}

        <div className="mt-2.5 flex items-baseline">
          <h3 className="text-[15px] font-bold">최근 활동</h3>
        </div>
        {activities.length === 0 && <p className="text-[12.5px] text-gray-500">아직 활동 내역이 없어요</p>}
        {activities.map((a) => (
          <ActivityRow key={a.id} activity={a} onClick={() => handleActivityClick(a)} />
        ))}
        {activities.length > 0 && (
          <Link
            href={`/plans/${planId}/activity`}
            className="mt-1 flex min-h-11 items-center justify-center rounded-xl bg-gray-100 text-[13px] font-bold text-gray-700"
          >
            활동 전체보기
          </Link>
        )}
      </div>
      <BottomTabBar planId={planId} />
      <Toast message={toastMessage} onClose={clearToast} />
    </div>
  );
}

function DayCard({
  planId,
  day,
  date,
  schedules,
}: {
  planId: string;
  day: number;
  date: string;
  schedules: Schedule[];
}) {
  const summary = schedules.length
    ? schedules
        .map((s) => (s.linkedVoteId ? `(투표중) ${s.placeName}` : s.placeName))
        .join(" → ")
    : "아직 일정이 없어요";

  return (
    <Link
      href={`/plans/${planId}/timeline?day=${day}`}
      className="flex w-full flex-col gap-1.5 rounded-2xl border border-gray-200 bg-white p-3.5 text-left"
    >
      <div className="text-[14px] font-bold text-primary">
        Day {day} · {formatDateShort(date)}
      </div>
      <div className="truncate text-[13px] text-gray-700">{summary}</div>
    </Link>
  );
}
