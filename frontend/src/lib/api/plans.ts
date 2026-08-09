//import { generateId } from "@/lib/utils";
//import { store, simulateLatency } from "./store";
import { apiFetch } from "./client";
import type { Plan, Role } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}
interface PlanApiResponse {
  planId: number;
  title: string;
  description: string | null;
  startDate: string;
  endDate: string;
  recruitmentCount: number | null;
  ownerId: number;
  myRole: Role;
  memberCount: number;
  createdAt: string;
}

function mapPlan(res: PlanApiResponse): Plan {
  return {
    id: String(res.planId),
    title: res.title,
    description: res.description ?? undefined,
    startDate: res.startDate,
    endDate: res.endDate,
    capacity: res.recruitmentCount ?? undefined,
    ownerId: String(res.ownerId),
    members: [],
    createdAt: res.createdAt,
    myRole: res.myRole,
    memberCount: res.memberCount,
  };
}

export interface CreatePlanInput {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  capacity?: number;
}

/** GET /api/v1/plans — 내가 참여중인 계획 목록 */
export async function getPlans(): Promise<Plan[]> {
  //await simulateLatency();
  //return store.plans.filter((p) => p.members.some((m) => m.userId === store.me.id));
  const response = await apiFetch<CommonResponse<PlanApiResponse[]>>("/api/v1/plans");
  return response.data.map(mapPlan);
}

/** GET /api/v1/plans/{planId} */
export async function getPlan(planId: string): Promise<Plan | null> {
  //await simulateLatency(150);
  //return store.getPlan(planId) ?? null;
  try {
    const response = await apiFetch<CommonResponse<PlanApiResponse>>(`/api/v1/plans/${planId}`);
    return mapPlan(response.data);
  } catch (error) {
    // ApiError에 status가 있습니다 (client.ts 참고). 404/403이면 "없음"으로 처리합니다.
    if (error instanceof Error && "status" in error) {
      const status = (error as { status: number }).status;
      if (status === 404 || status === 403) {
        return null;
      }
    }
    throw error;
  }
}

/** POST /api/v1/plans */
export async function createPlan(input: CreatePlanInput): Promise<Plan> {
  /*await simulateLatency(300);
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
  return plan;*/
  const response = await apiFetch<CommonResponse<PlanApiResponse>>("/api/v1/plans", {
    method: "POST",
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      startDate: input.startDate,
      endDate: input.endDate,
      recruitmentCount: input.capacity, // 프론트 필드명(capacity) -> 백엔드 필드명(recruitmentCount) 변환
    }),
  });
  return mapPlan(response.data);
}

/** PATCH /api/v1/plans/{planId} */
export async function updatePlan(planId: string, input: Partial<CreatePlanInput>): Promise<Plan> {
  /*await simulateLatency();
  const plan = store.getPlan(planId);
  if (!plan) throw new Error("Plan not found");
  Object.assign(plan, input);
  return plan;*/
  const response = await apiFetch<CommonResponse<PlanApiResponse>>(`/api/v1/plans/${planId}`, {
    method: "PATCH",
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      startDate: input.startDate,
      endDate: input.endDate,
      recruitmentCount: input.capacity,
    }),
  });
  return mapPlan(response.data);
}

export async function deletePlan(planId: string): Promise<void> {
  await apiFetch<CommonResponse<null>>(`/api/v1/plans/${planId}`, {
    method: "DELETE",
  });
}