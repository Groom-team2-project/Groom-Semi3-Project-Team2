import { apiFetch, USE_MOCK } from "./client";
import { simulateLatency, store } from "./store";
import type { ActivityLog } from "./types";

interface CommonResponse<T> { data: T; }

interface ActivityApiResponse {
  logId: number;
  planId: number;
  planTitle: string;
  actionType: string;
  targetType: string;
  targetId: number;
  scheduleId: number | null;
  summary: string;
  nickname: string;
  createdAt: string;
}

function mapActivity(response: ActivityApiResponse): ActivityLog {
  const actionTypeMap: Record<string, ActivityLog["type"]> = {
    SCHEDULE_CREATED: "schedule_added",
    SCHEDULE_UPDATED: "schedule_updated",
    SCHEDULE_DELETED: "schedule_deleted",
    VOTE_CREATED: "vote_created",
    VOTE_UPDATED: "vote_updated",
    VOTE_DELETED: "vote_deleted",
    VOTE_PARTICIPATED: "vote_participated",
    VOTE_CLOSED: "vote_closed",
    MEMBER_JOINED: "member_joined",
    MEMBER_LEFT: "member_left",
    MEMBER_ROLE_CHANGED: "member_role_changed",
    COMMENT_CREATED: "comment_added",
    COMMENT_DELETED: "comment_deleted",
  };

  return {
    id: String(response.logId),
    planId: String(response.planId),
    planTitle: response.planTitle,
    actorName: response.nickname,
    actorColor: "#8B95A1",
    actorInitial: response.nickname.slice(0, 1),
    type: actionTypeMap[response.actionType] ?? "comment_added",
    summary: response.summary,
    targetType: response.targetType.toLowerCase() as ActivityLog["targetType"],
    targetId: String(response.targetId),
    scheduleId: response.scheduleId ? String(response.scheduleId) : undefined,
    createdAt: response.createdAt,
  };
}

/** GET /api/v1/plans/{planId}/activities — activity_logs 테이블 기반, 계획 단위 최신순 조회 */
export async function getActivities(planId: string, limit?: number): Promise<ActivityLog[]> {
  if (USE_MOCK) {
    await simulateLatency(180);
    const sharedTypes: ActivityLog["type"][] = [
      "schedule_added", "schedule_updated", "schedule_deleted", "vote_created", "vote_updated", "vote_deleted", "vote_closed", "member_joined", "member_left", "member_role_changed", "comment_added",
    ];
    const activities = store.activities
      .filter((activity) => activity.planId === planId)
      .filter((activity) => sharedTypes.includes(activity.type))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map((activity) => {
        if (activity.targetType !== "comment" || !activity.targetId) return activity;
        const comment = store.comments.find((item) => item.id === activity.targetId);
        return { ...activity, scheduleId: comment?.scheduleId };
      });
    return typeof limit === "number" ? activities.slice(0, limit) : activities;
  }

  const response = await apiFetch<CommonResponse<ActivityApiResponse[]>>(`/api/v1/plans/${planId}/activities`);
  const activities = response.data.map(mapActivity);
  return typeof limit === "number" ? activities.slice(0, limit) : activities;
}

/** GET /api/v1/users/me/activities — 내 수정·삭제를 포함한 전체 활동 조회 */
export async function getMyActivities(): Promise<ActivityLog[]> {
  if (USE_MOCK) {
    await simulateLatency(180);
    return store.activities
      .filter((activity) => activity.actorName === store.me.name)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map((activity) => ({
        ...activity,
        planTitle: store.getPlan(activity.planId)?.title ?? "삭제된 계획",
      }));
  }

  const response = await apiFetch<CommonResponse<ActivityApiResponse[]>>("/api/v1/users/me/activities");
  return response.data.map(mapActivity);
}
