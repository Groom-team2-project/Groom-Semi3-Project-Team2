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
  targetDeleted: boolean;
  summary: string;
  nickname: string;
  createdAt: string;
}

interface ActivityPageApiResponse {
  activities: ActivityApiResponse[];
  nextCursorCreatedAt: string | null;
  nextCursorLogId: number | null;
  hasNext: boolean;
}

export interface ActivityCursor {
  createdAt: string;
  logId: string;
}

export interface ActivityPage {
  activities: ActivityLog[];
  nextCursor?: ActivityCursor;
  hasNext: boolean;
}

interface ActivityQuery {
  size?: number;
  cursor?: ActivityCursor;
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
    COMMENT_LIKED: "comment_liked",
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
    targetDeleted: response.targetDeleted,
    createdAt: response.createdAt,
  };
}

function toMockPage(activities: ActivityLog[], query: ActivityQuery): ActivityPage {
  const size = query.size ?? 20;
  const startIndex = query.cursor
    ? activities.findIndex((activity) => activity.id === query.cursor?.logId) + 1
    : 0;
  const pageActivities = activities.slice(Math.max(startIndex, 0), startIndex + size);
  const hasNext = startIndex + size < activities.length;
  const lastActivity = pageActivities.at(-1);

  return {
    activities: pageActivities,
    nextCursor: hasNext && lastActivity
      ? { createdAt: lastActivity.createdAt, logId: lastActivity.id }
      : undefined,
    hasNext,
  };
}

/** GET /api/v1/plans/{planId}/activities — activity_logs 테이블 기반, 커서 기준 최신순 조회 */
export async function getActivities(planId: string, query: ActivityQuery = {}): Promise<ActivityPage> {
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
        return { ...activity, scheduleId: comment?.scheduleId, targetDeleted: !comment || comment.deleted };
      });
    return toMockPage(activities, query);
  }

  const params = new URLSearchParams({ size: String(query.size ?? 20) });
  if (query.cursor) {
    params.set("cursorCreatedAt", query.cursor.createdAt);
    params.set("cursorLogId", query.cursor.logId);
  }
  const response = await apiFetch<CommonResponse<ActivityPageApiResponse>>(`/api/v1/plans/${planId}/activities?${params}`);
  return {
    activities: response.data.activities.map(mapActivity),
    nextCursor: response.data.hasNext && response.data.nextCursorCreatedAt && response.data.nextCursorLogId
      ? { createdAt: response.data.nextCursorCreatedAt, logId: String(response.data.nextCursorLogId) }
      : undefined,
    hasNext: response.data.hasNext,
  };
}

/** GET /api/v1/users/me/activities — 내 수정·삭제를 포함한 전체 활동 조회 */
export async function getMyActivities(query: ActivityQuery = {}): Promise<ActivityPage> {
  if (USE_MOCK) {
    await simulateLatency(180);
    const activities = store.activities
      .filter((activity) => activity.actorName === store.me.name)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map((activity) => {
        const planTitle = store.getPlan(activity.planId)?.title ?? "삭제된 계획";
        if (activity.targetType !== "comment" || !activity.targetId) return { ...activity, planTitle };
        const comment = store.comments.find((item) => item.id === activity.targetId);
        return { ...activity, planTitle, scheduleId: comment?.scheduleId, targetDeleted: !comment || comment.deleted };
      });
    return toMockPage(activities, query);
  }

  const params = new URLSearchParams({ size: String(query.size ?? 20) });
  if (query.cursor) {
    params.set("cursorCreatedAt", query.cursor.createdAt);
    params.set("cursorLogId", query.cursor.logId);
  }
  const response = await apiFetch<CommonResponse<ActivityPageApiResponse>>(`/api/v1/users/me/activities?${params}`);
  return {
    activities: response.data.activities.map(mapActivity),
    nextCursor: response.data.hasNext && response.data.nextCursorCreatedAt && response.data.nextCursorLogId
      ? { createdAt: response.data.nextCursorCreatedAt, logId: String(response.data.nextCursorLogId) }
      : undefined,
    hasNext: response.data.hasNext,
  };
}
