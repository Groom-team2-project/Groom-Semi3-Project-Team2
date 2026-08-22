import { generateId } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import { apiFetch, ApiError, USE_MOCK } from "./client";
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
  participantCount: number;
}

function mapOption(response: VoteOptionApiResponse): VoteOption {
  return {
    id: response.id,
    voteId: response.voteId,
    placeName: response.placeName,
    placeAddress: response.placeAddress ?? undefined,
    emoji: response.emoji,
    voteCount: response.voteCount,
  };
}

function mapVote(response: VoteApiResponse): Vote {
  return {
    id: response.id,
    planId: response.planId,
    title: response.title,
    status: response.status,
    deadline: response.deadline,
    options: response.options.map(mapOption),
    myOptionId: response.myOptionId ?? undefined,
    linkedScheduleId: response.linkedScheduleId ?? undefined,
    resultSummary: response.resultSummary ?? undefined,
  };
}

export interface CreateVoteInput {
  title: string;
  deadline: string; // ISO datetime
  options: Array<{ placeName: string; placeAddress?: string; emoji?: string }>;
}

/** GET /api/v1/plans/{planId}/votes */
export async function getVotes(planId: string): Promise<Vote[]> {
  if (USE_MOCK) {
    await simulateLatency(180);
    return store.votes.filter((v) => v.planId === planId);
  }

  const response = await apiFetch<CommonResponse<VoteApiResponse[]>>(
    `/api/v1/plans/${planId}/votes`,
  );
  return response.data.map(mapVote);
}

/** GET /api/v1/plans/{planId}/votes/{voteId} */
export async function getVote(planId: string, voteId: string): Promise<Vote | null> {
  if (USE_MOCK) {
    await simulateLatency(120);
    return store.votes.find((v) => v.planId === planId && v.id === voteId) ?? null;
  }

  try {
    const response = await apiFetch<CommonResponse<VoteApiResponse>>(
      `/api/v1/plans/${planId}/votes/${voteId}`,
    );
    return mapVote(response.data);
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
  if (USE_MOCK) {
    await simulateLatency(320);
    const voteId = generateId("vote");
    const vote: Vote = {
      id: voteId,
      planId,
      title: input.title,
      status: "OPEN",
      deadline: input.deadline,
      options: input.options.map((o) => ({
        id: generateId("vopt"),
        voteId,
        placeName: o.placeName,
        placeAddress: o.placeAddress,
        emoji: o.emoji ?? "🍽️",
        voteCount: 0,
      })),
    };
    store.votes.push(vote);
    store.recordActivity(planId, "vote_created", `'${vote.title}' 투표를 시작했어요`, "vote", vote.id);
    return vote;
  }

  const response = await apiFetch<CommonResponse<VoteApiResponse>>(
    `/api/v1/plans/${planId}/votes`,
    {
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
    },
  );
  return mapVote(response.data);
}

/** POST /api/v1/plans/{planId}/votes/{voteId}/participations — 단일 선택, 재투표 시 기존 표 교체 */
export async function castVote(planId: string, voteId: string, optionId: string): Promise<Vote> {
  if (USE_MOCK) {
    await simulateLatency(200);
    const vote = store.votes.find((v) => v.planId === planId && v.id === voteId);
    if (!vote) throw new Error("Vote not found");
    if (vote.status === "CLOSED") throw new Error("마감된 투표입니다");

    if (vote.myOptionId && vote.myOptionId !== optionId) {
      const prev = vote.options.find((o) => o.id === vote.myOptionId);
      if (prev) prev.voteCount = Math.max(0, prev.voteCount - 1);
    }
    if (vote.myOptionId !== optionId) {
      const next = vote.options.find((o) => o.id === optionId);
      if (next) next.voteCount += 1;
      vote.myOptionId = optionId;
    }
    return vote;
  }

  const response = await apiFetch<CommonResponse<VoteApiResponse>>(
    `/api/v1/plans/${planId}/votes/${voteId}/participations`,
    {
      method: "POST",
      body: JSON.stringify({ optionId }),
    },
  );
  return mapVote(response.data);
}
