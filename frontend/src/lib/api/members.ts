import { store, simulateLatency } from "./store";
import type { Invitation, Member, Role } from "./types";

/** GET /api/v1/plans/{planId}/members */
export async function getMembers(planId: string): Promise<Member[]> {
  await simulateLatency(150);
  return store.getPlan(planId)?.members ?? [];
}

/** PATCH /api/v1/plans/{planId}/members/{memberId} — 모임장이 편집자 ↔ 뷰어 전환 */
export async function updateMemberRole(planId: string, memberId: string, role: Role): Promise<Member> {
  await simulateLatency();
  const plan = store.getPlan(planId);
  const member = plan?.members.find((m) => m.id === memberId);
  if (!plan || !member) throw new Error("Member not found");
  member.role = role;
  return member;
}

/** POST /api/v1/plans/{planId}/invitations — 초대 링크 발급/조회 */
export async function getInvitation(planId: string): Promise<Invitation> {
  await simulateLatency(150);
  return {
    code: planId.slice(-6).toUpperCase(),
    url: `trip.app/j/${planId.slice(-6).toUpperCase()}`,
    expiresAt: new Date(Date.now() + 7 * 86_400_000).toISOString(),
  };
}

export async function reissueInvitation(planId: string): Promise<Invitation> {
  await simulateLatency(250);
  store.recordActivity(planId, "invitation_reissued", "초대 링크를 재발급했어요", "member");
  return {
    code: Math.random().toString(36).slice(2, 8).toUpperCase(),
    url: `trip.app/j/${Math.random().toString(36).slice(2, 8).toUpperCase()}`,
    expiresAt: new Date(Date.now() + 7 * 86_400_000).toISOString(),
  };
}
