import {
  INITIAL_ACTIVITIES,
  INITIAL_COMMENTS,
  INITIAL_PLACES,
  INITIAL_PLANS,
  INITIAL_SCHEDULES,
  INITIAL_VOTES,
  MOCK_ME,
} from "./mockData";
import type {
  ActivityLog,
  ActivityType,
  Comment,
  Place,
  Plan,
  Role,
  Schedule,
  User,
  Vote,
} from "./types";
import { generateId } from "@/lib/utils";

/**
 * 아주 단순한 인메모리 mock DB.
 * 백엔드가 생기면 이 파일과 mockData.ts는 통째로 지우고,
 * `../api/*.ts`의 각 함수 내부만 실제 fetch 호출로 교체하면 됩니다.
 *
 * 세션(브라우저 탭) 동안의 상태만 유지합니다 — 새로고침하면 초기화됩니다.
 */
class MockStore {
  me: User = MOCK_ME;
  plans: Plan[] = structuredClone(INITIAL_PLANS);
  places: Place[] = structuredClone(INITIAL_PLACES);
  schedules: Schedule[] = structuredClone(INITIAL_SCHEDULES);
  votes: Vote[] = structuredClone(INITIAL_VOTES);
  comments: Comment[] = structuredClone(INITIAL_COMMENTS);
  activities: ActivityLog[] = structuredClone(INITIAL_ACTIVITIES);

  recordActivity(
    planId: string,
    type: ActivityType,
    summary: string,
    targetType?: ActivityLog["targetType"],
    targetId?: string,
  ) {
    const log: ActivityLog = {
      id: generateId("act"),
      planId,
      actorName: this.me.name,
      actorColor: this.me.avatarColor,
      actorInitial: this.me.avatarInitial,
      type,
      summary,
      targetType,
      targetId,
      createdAt: new Date().toISOString(),
    };
    this.activities.unshift(log);
    return log;
  }

  getPlan(planId: string): Plan | undefined {
    return this.plans.find((p) => p.id === planId);
  }

  requireMemberRole(planId: string): Role {
    const plan = this.getPlan(planId);
    const member = plan?.members.find((m) => m.userId === this.me.id);
    return member?.role ?? "VIEWER";
  }
}

// globalThis에 caching해서 개발 모드 HMR 시에도 상태가 리셋되지 않도록 함
const globalForStore = globalThis as unknown as { __tripmateStore?: MockStore };

export const store: MockStore = globalForStore.__tripmateStore ?? new MockStore();
if (process.env.NODE_ENV !== "production") {
  globalForStore.__tripmateStore = store;
}

export function simulateLatency(ms = 220): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
