import { apiFetch, ApiError } from "./client";
import type { Vote, VoteOption } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}

/** 백엔드 VoteOptionResponse. ID는 모두 문자열로 내려옵니다. */
interface VoteOptionApiResponse {
  id: string;
  voteId: string;
  placeName: string;
  placeAddress: string | null;
  emoji: string;
  voteCount: number;
  placeId: string | null;
  selectedByMe: boolean;
}

/** 백엔드 VoteResponse. 화면이 쓰지 않는 필드도 함께 오지만 여기서 걸러 냅니다. */
interface VoteApiResponse {
  id: string;
  planId: string;
  title: string;
  status: "OPEN" | "CLOSED";
  deadline: string;
  options: VoteOptionApiResponse[];
  myOptionId: string | null;
  linkedScheduleId: string | null;
  resultSummary: string | null;
  creatorId: string;
  participantCount: number;
}

function mapOption(res: VoteOptionApiResponse): VoteOption {
  return {
    id: res.id,
    voteId: res.voteId,
    placeName: res.placeName,
    placeAddress: res.placeAddress ?? undefined,
    emoji: res.emoji,
    voteCount: res.voteCount,
  };
}

function mapVote(res: VoteApiResponse): Vote {
  return {
    id: res.id,
    planId: res.planId,
    title: res.title,
    status: res.status,
    deadline: res.deadline,
    options: res.options.map(mapOption),
    creatorId: res.creatorId,
    myOptionId: res.myOptionId ?? undefined,
    linkedScheduleId: res.linkedScheduleId ?? undefined,
    resultSummary: res.resultSummary ?? undefined,
  };
}

export interface CreateVoteInput {
  title: string;
  deadline: string; // ISO datetime
  options: Array<{ placeName: string; placeAddress?: string; emoji?: string }>;
}

/** GET /api/v1/plans/{planId}/votes */
export async function getVotes(planId: string): Promise<Vote[]> {
  const res = await apiFetch<CommonResponse<VoteApiResponse[]>>(`/api/v1/plans/${planId}/votes`);
  return res.data.map(mapVote);
}

/** GET /api/v1/plans/{planId}/votes/{voteId} */
export async function getVote(planId: string, voteId: string): Promise<Vote | null> {
  try {
    const res = await apiFetch<CommonResponse<VoteApiResponse>>(
      `/api/v1/plans/${planId}/votes/${voteId}`,
    );
    return mapVote(res.data);
  } catch (error) {
    // 없는 투표거나 이 계획의 투표가 아니면 "존재하지 않는 투표" 화면을 보여줍니다.
    if (error instanceof ApiError && (error.status === 404 || error.status === 400)) {
      return null;
    }
    throw error;
  }
}

/** POST /api/v1/plans/{planId}/votes — 선택지까지 한 번에 만듭니다. */
export async function createVote(planId: string, input: CreateVoteInput): Promise<Vote> {
  const res = await apiFetch<CommonResponse<VoteApiResponse>>(`/api/v1/plans/${planId}/votes`, {
    method: "POST",
    body: JSON.stringify({
      title: input.title,
      deadline: input.deadline,
      options: input.options.map((o) => ({
        placeName: o.placeName,
        placeAddress: o.placeAddress,
        emoji: o.emoji,
      })),
    }),
  });
  return mapVote(res.data);
}

/** POST /api/v1/plans/{planId}/votes/{voteId}/participations — 단일 선택, 재투표 시 기존 표 교체 */
export async function castVote(planId: string, voteId: string, optionId: string): Promise<Vote> {
  const res = await apiFetch<CommonResponse<VoteApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}/participations`,
    { method: "POST", body: JSON.stringify({ optionId }) },
  );
  return mapVote(res.data);
}

export interface UpdateVoteInput {
  title?: string;
  deadline?: string; // ISO datetime
}

export interface VoteOptionInput {
  placeName: string;
  placeAddress?: string;
  emoji?: string;
}

/** PATCH /api/v1/plans/{planId}/votes/{voteId} — 생성자만, 진행 중일 때만 */
export async function updateVote(
  planId: string,
  voteId: string,
  input: UpdateVoteInput,
): Promise<Vote> {
  const res = await apiFetch<CommonResponse<VoteApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}`,
    { method: "PATCH", body: JSON.stringify(input) },
  );
  return mapVote(res.data);
}

/** POST /api/v1/plans/{planId}/votes/{voteId}/close — 마감 일시 전에 생성자가 직접 마감 */
export async function closeVote(planId: string, voteId: string): Promise<Vote> {
  const res = await apiFetch<CommonResponse<VoteApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}/close`,
    { method: "POST" },
  );
  return mapVote(res.data);
}

/** DELETE /api/v1/plans/{planId}/votes/{voteId} — 선택지와 투표 기록도 함께 사라집니다. */
export async function deleteVote(planId: string, voteId: string): Promise<void> {
  await apiFetch<void>(`/api/v1/plans/${planId}/votes/${voteId}`, { method: "DELETE" });
}

/** POST /api/v1/plans/{planId}/votes/{voteId}/options */
export async function addVoteOption(
  planId: string,
  voteId: string,
  input: VoteOptionInput,
): Promise<VoteOption> {
  const res = await apiFetch<CommonResponse<VoteOptionApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}/options`,
    { method: "POST", body: JSON.stringify(input) },
  );
  return mapOption(res.data);
}

/**
 * PATCH /api/v1/plans/{planId}/votes/{voteId}/options/{optionId}
 *
 * placeId를 생략하면 기존 장소 연결이 유지됩니다. 연결을 끊으려면 clearPlace를 보냅니다.
 */
export async function updateVoteOption(
  planId: string,
  voteId: string,
  optionId: string,
  input: VoteOptionInput & { clearPlace?: boolean },
): Promise<VoteOption> {
  const res = await apiFetch<CommonResponse<VoteOptionApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}/options/${optionId}`,
    { method: "PATCH", body: JSON.stringify(input) },
  );
  return mapOption(res.data);
}

/** DELETE /api/v1/plans/{planId}/votes/{voteId}/options/{optionId} — 후보는 2개 아래로 줄일 수 없습니다. */
export async function deleteVoteOption(
  planId: string,
  voteId: string,
  optionId: string,
): Promise<void> {
  await apiFetch<void>(`/api/v1/plans/${planId}/votes/${voteId}/options/${optionId}`, {
    method: "DELETE",
  });
}

/** DELETE /api/v1/plans/{planId}/votes/{voteId}/participations — 내 표를 무릅니다. */
export async function cancelVote(planId: string, voteId: string): Promise<void> {
  await apiFetch<void>(`/api/v1/plans/${planId}/votes/${voteId}/participations`, {
    method: "DELETE",
  });
}
