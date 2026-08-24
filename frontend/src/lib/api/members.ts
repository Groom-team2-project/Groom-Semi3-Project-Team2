import { apiFetch } from "./client";
import type { Invitation, Member, Role } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}

interface MemberApiResponse {
  memberId: number;
  userId: number;
  nickname: string;
  profileImage: string | null;
  role: Role;
  status: "JOINED" | "LEFT";
  joinedAt: string | null;
}

const AVATAR_COLORS = ["#3182F6", "#00C896", "#FF9F1C", "#8B7FF2", "#F45B69"];

function mapMember(res: MemberApiResponse): Member {
  return {
    id: String(res.memberId),
    userId: String(res.userId),
    name: res.nickname,
    avatarColor: AVATAR_COLORS[res.userId % AVATAR_COLORS.length],
    avatarInitial: res.nickname.charAt(0),
    role: res.role,
    status: res.status,
  };
}
/** GET /api/v1/plans/{planId}/members */
export async function getMembers(planId: string): Promise<Member[]> {
  const response = await apiFetch<CommonResponse<MemberApiResponse[]>>(
      `/api/v1/plans/${planId}/members`,
  );
  return response.data.map(mapMember);
}

/** PATCH /api/v1/plans/{planId}/members/{memberId} — 모임장이 편집자 ↔ 뷰어 전환 */
export async function updateMemberRole(planId: string, memberId: string, role: Role): Promise<Member> {
  const response = await apiFetch<CommonResponse<MemberApiResponse>>(
      `/api/v1/plans/${planId}/members/${memberId}/role`,
      {
        method: "PATCH",
        body: JSON.stringify({ role }),
      },
  );
  return mapMember(response.data);
}

export async function removeMember(planId: string, memberId: string): Promise<void> {
  await apiFetch<CommonResponse<null>>(`/api/v1/plans/${planId}/members/${memberId}`, {
    method: "DELETE",
  });
}

export async function leavePlan(planId: string): Promise<void> {
  await apiFetch<CommonResponse<null>>(`/api/v1/plans/${planId}/members/me`, {
    method: "DELETE",
  });
}

interface InvitationApiResponse {
  invitationId: number;
  planId: number;
  inviteCode: string;
  status: "ACTIVE" | "EXPIRED" | "REVOKED";
  expiresAt: string;
}

function mapInvitation(res: InvitationApiResponse): Invitation {
  const appUrl =
      (typeof window !== "undefined"
              ? window.location.origin
              : process.env.NEXT_PUBLIC_APP_URL?.trim() ?? ""
      ).replace(/\/+$/, "");

  return {
    code: res.inviteCode,
    url: `${appUrl}/invitations/${res.inviteCode}`,
    expiresAt: res.expiresAt,
    status: res.status,
    planId: res.planId,
    invitationId: String(res.invitationId)
  };
}

/** POST /api/v1/plans/{planId}/invitations — 초대 링크 발급/조회 */
export async function getInvitation(planId: string): Promise<Invitation> {
  const response = await apiFetch<CommonResponse<InvitationApiResponse>>(
      `/api/v1/plans/${planId}/invitations`,
      { method: "POST" },
  );
  return mapInvitation(response.data);
}

export async function reissueInvitation(planId: string): Promise<Invitation> {
  const response = await apiFetch<CommonResponse<InvitationApiResponse>>(
      `/api/v1/plans/${planId}/invitations/reissue`,
      { method: "POST" },
  );
  return mapInvitation(response.data);
}