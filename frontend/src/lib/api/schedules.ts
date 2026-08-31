import { generateId, dayIndexToDate } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import { apiFetch, USE_MOCK } from "./client";
import type { Comment, Schedule } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}

interface CommentApiResponse {
  commentId: number;
  planId: number;
  scheduleId: number;
  parentCommentId: number | null;
  content: string;
  deleted: boolean;
  userId: number | null;
  nickname: string | null;
  profileImage: string | null;
  createdAt: string;
  likeCount: number;
  likedByMe: boolean;
}

interface CommentLikeApiResponse {
  commentId: number;
  likeCount: number;
  likedByMe: boolean;
}

function mapComment(response: CommentApiResponse): Comment {
  return {
    id: String(response.commentId),
    planId: String(response.planId),
    scheduleId: String(response.scheduleId),
    parentCommentId: response.parentCommentId ? String(response.parentCommentId) : undefined,
    userId: response.userId ? String(response.userId) : undefined,
    authorName: response.nickname ?? "삭제된 사용자",
    authorColor: "#8B95A1",
    profileImage: response.profileImage ?? undefined,
    text: response.content,
    deleted: response.deleted,
    createdAt: response.createdAt,
    likeCount: response.likeCount,
    likedByMe: response.likedByMe,
  };
}

export interface UpsertScheduleInput {
  day: number;
  time: string;
  placeName: string;
  placeAddress?: string;
  emoji?: string;
  memo?: string;
}

/** GET /api/v1/plans/{planId}/schedules */
export async function getSchedules(planId: string): Promise<Schedule[]> {
  await simulateLatency(180);
  return store.schedules
    .filter((s) => s.planId === planId)
    .sort((a, b) => a.day - b.day || a.time.localeCompare(b.time));
}

export async function getSchedulesByDay(planId: string, day: number): Promise<Schedule[]> {
  const all = await getSchedules(planId);
  return all.filter((s) => s.day === day);
}

/** GET /api/v1/plans/{planId}/schedules/{scheduleId} */
export async function getSchedule(planId: string, scheduleId: string): Promise<Schedule | null> {
  await simulateLatency(120);
  return store.schedules.find((s) => s.planId === planId && s.id === scheduleId) ?? null;
}

/** POST /api/v1/plans/{planId}/schedules */
export async function createSchedule(planId: string, input: UpsertScheduleInput): Promise<Schedule> {
  await simulateLatency(300);
  const plan = store.getPlan(planId);
  const schedule: Schedule = {
    id: generateId("sch"),
    planId,
    day: input.day,
    date: plan ? dayIndexToDate(plan.startDate, input.day) : new Date().toISOString().slice(0, 10),
    time: input.time,
    placeName: input.placeName,
    placeAddress: input.placeAddress,
    emoji: input.emoji ?? "📍",
    memo: input.memo,
  };
  store.schedules.push(schedule);
  store.recordActivity(planId, "schedule_added", `'${schedule.placeName}' 일정을 추가했어요`, "schedule", schedule.id);
  return schedule;
}

/** PATCH /api/v1/plans/{planId}/schedules/{scheduleId} */
export async function updateSchedule(
  planId: string,
  scheduleId: string,
  input: Partial<UpsertScheduleInput>,
): Promise<Schedule> {
  await simulateLatency();
  const schedule = store.schedules.find((s) => s.planId === planId && s.id === scheduleId);
  if (!schedule) throw new Error("Schedule not found");
  Object.assign(schedule, input);
  store.recordActivity(planId, "schedule_updated", `'${schedule.placeName}' 일정을 수정했어요`, "schedule", schedule.id);
  return schedule;
}

/** DELETE /api/v1/plans/{planId}/schedules/{scheduleId} */
export async function deleteSchedule(planId: string, scheduleId: string): Promise<void> {
  await simulateLatency(250);
  const schedule = store.schedules.find((s) => s.planId === planId && s.id === scheduleId);
  store.schedules = store.schedules.filter((s) => !(s.planId === planId && s.id === scheduleId));
  store.comments = store.comments.filter((c) => c.scheduleId !== scheduleId);
  if (schedule) {
    store.recordActivity(planId, "schedule_deleted", `'${schedule.placeName}' 일정을 삭제했어요`, "schedule", schedule.id);
  }
}

/** GET /api/v1/plans/{planId}/schedules/{scheduleId}/comments */
export async function getComments(planId: string, scheduleId: string): Promise<Comment[]> {
  if (USE_MOCK) {
    await simulateLatency(150);
    return store.comments
      .filter((comment) => comment.scheduleId === scheduleId)
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  const response = await apiFetch<CommonResponse<CommentApiResponse[]>>(
    `/api/v1/plans/${planId}/schedules/${scheduleId}/comments`,
  );
  return response.data.map(mapComment);
}

/** POST /api/v1/plans/{planId}/schedules/{scheduleId}/comments */
export async function addComment(
  planId: string,
  scheduleId: string,
  content: string,
  parentCommentId?: string,
): Promise<Comment> {
  if (USE_MOCK) {
    await simulateLatency(220);
    const comment: Comment = {
      id: generateId("cmt"),
      planId,
      scheduleId,
      parentCommentId,
      userId: store.me.id,
      authorName: store.me.name,
      authorColor: store.me.avatarColor,
      text: content,
      createdAt: new Date().toISOString(),
      likeCount: 0,
      likedByMe: false,
    };
    store.comments.push(comment);
    store.recordActivity(
      planId,
      "comment_added",
      parentCommentId ? "댓글에 답글을 남겼어요" : "댓글을 남겼어요",
      "comment",
      comment.id,
    );
    return comment;
  }

  const response = await apiFetch<CommonResponse<CommentApiResponse>>(
    `/api/v1/plans/${planId}/schedules/${scheduleId}/comments`,
    {
      method: "POST",
      body: JSON.stringify({ content, parentCommentId: parentCommentId ? Number(parentCommentId) : null }),
    },
  );
  return mapComment(response.data);
}

/** DELETE /api/v1/plans/{planId}/schedules/{scheduleId}/comments/{commentId} */
export async function deleteComment(planId: string, scheduleId: string, commentId: string): Promise<void> {
  if (USE_MOCK) {
    await simulateLatency(150);
    const comment = store.comments.find((item) => item.id === commentId && item.scheduleId === scheduleId);
    if (comment) {
      comment.deleted = true;
      comment.text = "삭제된 댓글입니다.";
      store.recordActivity(planId, "comment_deleted", "댓글을 삭제했어요", "comment", commentId);
    }
    return;
  }

  await apiFetch<CommonResponse<void>>(
    `/api/v1/plans/${planId}/schedules/${scheduleId}/comments/${commentId}`,
    { method: "DELETE" },
  );
}

/** POST /api/v1/plans/{planId}/schedules/{scheduleId}/comments/{commentId}/likes */
export async function toggleCommentLike(
  planId: string,
  scheduleId: string,
  commentId: string,
): Promise<{ likeCount: number; likedByMe: boolean }> {
  if (USE_MOCK) {
    await simulateLatency(120);
    const comment = store.comments.find((item) => item.id === commentId && item.scheduleId === scheduleId);
    if (!comment) throw new Error("Comment not found");
    comment.likedByMe = !comment.likedByMe;
    comment.likeCount += comment.likedByMe ? 1 : -1;
    // 좋아요를 취소할 때는 기록하지 않음 (실제 백엔드와 동일한 정책)
    if (comment.likedByMe) {
      store.recordActivity(planId, "comment_liked", "댓글에 좋아요를 눌렀어요", "comment", commentId);
    }
    return { likeCount: comment.likeCount, likedByMe: comment.likedByMe };
  }

  const response = await apiFetch<CommonResponse<CommentLikeApiResponse>>(
    `/api/v1/plans/${planId}/schedules/${scheduleId}/comments/${commentId}/likes`,
    { method: "POST" },
  );
  return { likeCount: response.data.likeCount, likedByMe: response.data.likedByMe };
}
