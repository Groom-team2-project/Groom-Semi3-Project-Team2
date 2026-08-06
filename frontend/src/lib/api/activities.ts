import { store, simulateLatency } from "./store";
import type { ActivityLog } from "./types";

/** GET /api/v1/plans/{planId}/activities — activity_logs 테이블 기반, 계획 단위 최신순 조회 */
export async function getActivities(planId: string, limit?: number): Promise<ActivityLog[]> {
  await simulateLatency(180);
  const sorted = store.activities
    .filter((a) => a.planId === planId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  return typeof limit === "number" ? sorted.slice(0, limit) : sorted;
}
