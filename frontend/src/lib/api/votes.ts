import { generateId } from "@/lib/utils";
import { store, simulateLatency } from "./store";
import type { Vote } from "./types";

export interface CreateVoteInput {
  title: string;
  deadline: string; // ISO datetime
  options: Array<{ placeName: string; placeAddress?: string; emoji?: string }>;
}

/** GET /api/v1/plans/{planId}/votes */
export async function getVotes(planId: string): Promise<Vote[]> {
  await simulateLatency(180);
  return store.votes.filter((v) => v.planId === planId);
}

/** GET /api/v1/plans/{planId}/votes/{voteId} */
export async function getVote(planId: string, voteId: string): Promise<Vote | null> {
  await simulateLatency(120);
  return store.votes.find((v) => v.planId === planId && v.id === voteId) ?? null;
}

/** POST /api/v1/plans/{planId}/votes (+ /options) */
export async function createVote(planId: string, input: CreateVoteInput): Promise<Vote> {
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

/** POST /api/v1/plans/{planId}/votes/{voteId}/participations — 단일 선택, 재투표 시 기존 표 교체 */
export async function castVote(planId: string, voteId: string, optionId: string): Promise<Vote> {
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
    store.recordActivity(planId, "vote_participated", "투표에 참여했어요", "vote", vote.id);
  }
  return vote;
}
