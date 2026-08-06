"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Tag } from "@/components/ui/Tag";
import { VoteOptionBar } from "@/components/plan/VoteCard";
import { getVote, castVote } from "@/lib/api";
import type { Vote } from "@/lib/api";
import { formatDeadline } from "@/lib/utils";

export default function VoteDetailPage({ params }: { params: Promise<{ planId: string; voteId: string }> }) {
  const { planId, voteId } = use(params);
  const [vote, setVote] = useState<Vote | null | undefined>(undefined);
  const [casting, setCasting] = useState<string | null>(null);

  useEffect(() => {
    getVote(planId, voteId).then(setVote);
  }, [planId, voteId]);

  async function handleCast(optionId: string) {
    if (!vote || vote.status === "CLOSED" || casting) return;
    setCasting(optionId);
    try {
      const updated = await castVote(planId, voteId, optionId);
      setVote(updated);
    } finally {
      setCasting(null);
    }
  }

  if (vote === undefined) return null;
  if (vote === null) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="투표" backHref={`/plans/${planId}/votes`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">존재하지 않는 투표예요</p>
      </div>
    );
  }

  const totalVotes = vote.options.reduce((sum, o) => sum + o.voteCount, 0);
  const myOption = vote.options.find((o) => o.id === vote.myOptionId);

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title={vote.title} backHref={`/plans/${planId}/votes`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        {vote.status === "OPEN" ? (
          <Tag color="orange" className="self-start">
            ⏰ {formatDeadline(vote.deadline)}
          </Tag>
        ) : (
          <Tag color="gray" className="self-start">
            마감된 투표
          </Tag>
        )}
        <div className="text-[12px] text-gray-500">
          참여자 1인 1표{myOption ? ` · 내 투표: ${myOption.placeName}` : " · 아직 투표하지 않았어요"}
        </div>

        <div className="flex flex-col gap-2.5">
          {vote.options.map((opt) => (
            <button
              key={opt.id}
              type="button"
              onClick={() => handleCast(opt.id)}
              disabled={vote.status === "CLOSED" || Boolean(casting)}
              className="text-left disabled:cursor-default"
            >
              <VoteOptionBar label={opt.placeName} count={opt.voteCount} total={totalVotes} mine={opt.id === vote.myOptionId} />
            </button>
          ))}
        </div>

        {vote.status === "OPEN" ? (
          <p className="text-center text-[12px] text-gray-500">후보를 눌러 투표하거나 표를 바꿀 수 있어요</p>
        ) : (
          <Button href={`/plans/${planId}/votes`} variant="soft">
            {vote.resultSummary ?? "투표 결과 확인 완료"}
          </Button>
        )}
      </div>
    </div>
  );
}
