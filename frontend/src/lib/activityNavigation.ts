import { getComments, getSchedule } from "@/lib/api";
import type { ActivityLog } from "@/lib/api";

export async function getActivityDestination(planId: string, activity: ActivityLog): Promise<string | null> {
  if (activity.targetType === "schedule" && activity.targetId) {
    const schedule = await getSchedule(planId, activity.targetId);
    return schedule ? `/plans/${planId}/schedules/${activity.targetId}` : null;
  }

  if (activity.targetType === "comment" && activity.targetId && activity.scheduleId) {
    const comments = await getComments(planId, activity.scheduleId);
    const comment = comments.find((item) => item.id === activity.targetId);
    return comment && !comment.deleted
      ? `/plans/${planId}/schedules/${activity.scheduleId}?commentId=${activity.targetId}`
      : null;
  }

  if (activity.targetType === "vote" && activity.targetId) return `/plans/${planId}/votes/${activity.targetId}`;
  if (activity.targetType === "member") return `/plans/${planId}/members`;
  return null;
}
