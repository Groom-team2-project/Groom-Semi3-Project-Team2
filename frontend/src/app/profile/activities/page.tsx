"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { Toast } from "@/components/ui/Toast";
import { ActivityRow } from "@/components/plan/ActivityRow";
import { getMyActivities } from "@/lib/api";
import { getActivityDestination } from "@/lib/activityNavigation";
import type { ActivityLog } from "@/lib/api";

export default function MyActivityPage() {
  const router = useRouter();
  const [activities, setActivities] = useState<ActivityLog[] | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    getMyActivities().then(setActivities);
  }, []);

  const clearToast = useCallback(() => setToastMessage(null), []);

  async function handleClick(activity: ActivityLog) {
    const destination = await getActivityDestination(activity.planId, activity);
    if (destination) {
      router.push(destination);
      return;
    }
    setToastMessage("삭제되었거나 더 이상 볼 수 없는 활동이에요.");
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="내 활동 내역" backHref="/profile" />
      <div className="flex flex-1 flex-col gap-1 px-4 pb-8">
        {activities?.length === 0 && <EmptyState emoji="🕘" title="아직 활동 내역이 없어요" />}
        {activities?.map((activity) => (
          <div key={activity.id}>
            <p className="pt-3 text-[11.5px] font-medium text-gray-500">{activity.planTitle}</p>
            <ActivityRow activity={activity} onClick={() => handleClick(activity)} />
          </div>
        ))}
      </div>
      <Toast message={toastMessage} onClose={clearToast} />
    </div>
  );
}
