import { generateId, dayIndexToDate } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import type { Comment, Schedule } from "./types";

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
    store.recordActivity(planId, "schedule_deleted", `'${schedule.placeName}' 일정을 삭제했어요`, "schedule");
  }
}

/** GET /api/v1/plans/{planId}/schedules/{scheduleId}/comments */
export async function getComments(scheduleId: string): Promise<Comment[]> {
  await simulateLatency(150);
  return store.comments
    .filter((c) => c.scheduleId === scheduleId)
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

/** POST /api/v1/plans/{planId}/schedules/{scheduleId}/comments */
export async function addComment(planId: string, scheduleId: string, text: string): Promise<Comment> {
  await simulateLatency(220);
  const comment: Comment = {
    id: generateId("cmt"),
    scheduleId,
    authorName: store.me.name,
    authorColor: store.me.avatarColor,
    text,
    createdAt: new Date().toISOString(),
  };
  store.comments.push(comment);
  store.recordActivity(planId, "comment_added", "댓글을 남겼어요", "schedule", scheduleId);
  return comment;
}
