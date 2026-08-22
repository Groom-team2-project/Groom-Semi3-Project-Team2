"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { Toast } from "@/components/ui/Toast";
import { ActivityRow } from "@/components/plan/ActivityRow";
import { getMyActivities } from "@/lib/api";
import { getActivityDestination } from "@/lib/activityNavigation";
import { ApiError } from "@/lib/api/client";
import type { ActivityCursor, ActivityLog } from "@/lib/api";

function getActivityLoadMoreError(error: unknown): string {
  if (!(error instanceof ApiError)) return "활동을 더 불러오지 못했어요. 다시 시도해 주세요.";

  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return "활동 내역을 볼 수 있는 권한이 없어요.";
  if (error.status === 404) return "더 이상 볼 수 있는 활동이 없어요.";
  if (error.status === 400) return "요청이 올바르지 않아요. 새로고침 후 다시 시도해 주세요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";

  return "활동을 더 불러오지 못했어요. 다시 시도해 주세요.";
}

export default function MyActivityPage() {
  const router = useRouter();
  const [activities, setActivities] = useState<ActivityLog[] | null>(null);
  const [nextCursor, setNextCursor] = useState<ActivityCursor | undefined>();
  const [hasNext, setHasNext] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    getMyActivities().then((page) => {
      setActivities(page.activities);
      setNextCursor(page.nextCursor);
      setHasNext(page.hasNext);
    });
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

  async function loadMore() {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true);
    try {
      const page = await getMyActivities({ cursor: nextCursor });
      setActivities((previous) => [...(previous ?? []), ...page.activities]);
      setNextCursor(page.nextCursor);
      setHasNext(page.hasNext);
    } catch (error) {
      setToastMessage(getActivityLoadMoreError(error));
    } finally {
      setLoadingMore(false);
    }
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
        {hasNext && (
          <button
            type="button"
            className="mt-2 rounded-xl bg-gray-100 py-2.5 text-[13px] font-bold text-gray-700 disabled:opacity-50"
            onClick={loadMore}
            disabled={loadingMore}
          >
            {loadingMore ? "불러오는 중..." : "더 보기"}
          </button>
        )}
      </div>
      <Toast message={toastMessage} onClose={clearToast} />
    </div>
  );
}
