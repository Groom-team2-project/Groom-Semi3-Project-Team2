import { generateId } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import type { Plan } from "./types";

export interface CreatePlanInput {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  capacity?: number;
}

/** GET /api/v1/plans — 내가 참여중인 계획 목록 */
export async function getPlans(): Promise<Plan[]> {
  await simulateLatency();
  return store.plans.filter((p) => p.members.some((m) => m.userId === store.me.id));
}

/** GET /api/v1/plans/{planId} */
export async function getPlan(planId: string): Promise<Plan | null> {
  await simulateLatency(150);
  return store.getPlan(planId) ?? null;
}

/** POST /api/v1/plans */
export async function createPlan(input: CreatePlanInput): Promise<Plan> {
  await simulateLatency(300);
  const plan: Plan = {
    id: generateId("plan"),
    title: input.title,
    description: input.description,
    startDate: input.startDate,
    endDate: input.endDate,
    capacity: input.capacity,
    ownerId: store.me.id,
    members: [
      {
        id: generateId("mem"),
        userId: store.me.id,
        name: store.me.name,
        avatarColor: store.me.avatarColor,
        avatarInitial: store.me.avatarInitial,
        role: "OWNER",
      },
    ],
    createdAt: new Date().toISOString(),
  };
  store.plans.unshift(plan);
  return plan;
}

/** PATCH /api/v1/plans/{planId} */
export async function updatePlan(planId: string, input: Partial<CreatePlanInput>): Promise<Plan> {
  await simulateLatency();
  const plan = store.getPlan(planId);
  if (!plan) throw new Error("Plan not found");
  Object.assign(plan, input);
  return plan;
}
