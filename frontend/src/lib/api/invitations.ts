import { apiFetch } from "./client";
import type { Invitation, Role } from "./types";

interface CommonResponse<T> {
    success: boolean;
    data: T;
    errorCode: string | null;
    message: string;
}

// [삭제] role 제거
interface InvitationApiResponse {
    invitationId: number;
    inviteCode: string;
    status: "ACTIVE" | "EXPIRED" | "REVOKED";
    expiresAt: string;
}

interface JoinResponse {
    planId: number;
    memberId: number;
    role: Role; // 참여 후 실제로 부여된 role은 여기서 내려옵니다 (InvitationJoinResponse 기준).
    // 초대장 자체(Invitation)에는 role이 없지만, join 결과(MemberEntity)에는
    // role이 있는 게 맞습니다 — 이건 "이미 만들어진 멤버"의 속성이라 별개입니다.
}

function mapInvitation(res: InvitationApiResponse): Invitation {
    const appUrl = process.env.NEXT_PUBLIC_APP_URL ?? "";
    return {
        code: res.inviteCode,
        url: `${appUrl}/j/${res.inviteCode}`,
        expiresAt: res.expiresAt,
        status: res.status,
        invitationId: String(res.invitationId),
    };
}

export async function getInvitationByCode(inviteCode: string): Promise<Invitation> {
    const response = await apiFetch<CommonResponse<InvitationApiResponse>>(
        `/api/v1/invitations/${inviteCode}`,
    );
    return mapInvitation(response.data);
}

export async function joinByInviteCode(inviteCode: string): Promise<JoinResponse> {
    const response = await apiFetch<CommonResponse<JoinResponse>>(
        `/api/v1/invitations/${inviteCode}/join`,
        { method: "POST" },
    );
    return response.data;
}