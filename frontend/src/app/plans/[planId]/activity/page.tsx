"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { ActivityRow } from "@/components/plan/ActivityRow";
import { getActivities } from "@/lib/api";
import type { ActivityLog } from "@/lib/api";

export default function ActivityLogPage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const router = useRouter();
  const [activities, setActivities] = useState<ActivityLog[] | null>(null);

  useEffect(() => {
    getActivities(planId).then(setActivities);
  }, [planId]);

  function handleClick(activity: ActivityLog) {
    if (activity.targetType === "schedule" && activity.targetId) {
      router.push(`/plans/${planId}/schedules/${activity.targetId}`);
    } else if (activity.targetType === "vote" && activity.targetId) {
      router.push(`/plans/${planId}/votes/${activity.targetId}`);
    } else if (activity.targetType === "member") {
      router.push(`/plans/${planId}/members`);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="활동 내역" backHref={`/plans/${planId}`} />
      <div className="flex flex-1 flex-col gap-1 px-4 pb-8">
        {activities?.length === 0 && <EmptyState emoji="🕘" title="아직 활동 내역이 없어요" />}
        {activities?.map((a) => (
          <ActivityRow key={a.id} activity={a} onClick={() => handleClick(a)} />
        ))}
        {activities && activities.length > 0 && (
          <p className="pt-2 text-center text-[11.5px] text-gray-500">
            activity_logs 테이블 기반 · 계획 단위로 최신순 조회
          </p>
        )}
      </div>
    </div>
  );
}
