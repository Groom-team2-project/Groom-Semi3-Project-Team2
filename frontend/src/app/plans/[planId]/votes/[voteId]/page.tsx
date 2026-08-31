"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Tag } from "@/components/ui/Tag";
import { VoteOptionBar } from "@/components/plan/VoteCard";
import { useAuth } from "@/context/AuthContext";
import { getVote, castVote, cancelVote, closeVote } from "@/lib/api";
import type { Vote } from "@/lib/api";
import { formatDeadline } from "@/lib/utils";

export default function VoteDetailPage({ params }: { params: Promise<{ planId: string; voteId: string }> }) {
  const { planId, voteId } = use(params);
  const { user } = useAuth();
  const [vote, setVote] = useState<Vote | null | undefined>(undefined);
  const [casting, setCasting] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    getVote(planId, voteId).then(setVote);
  }, [planId, voteId]);

  async function handleCast(optionId: string) {
    if (!vote || vote.status === "CLOSED" || casting) return;
    setCasting(optionId);
    try {
      setVote(await castVote(planId, voteId, optionId));
    } finally {
      setCasting(null);
    }
  }

  async function handleCancelVote() {
    if (!vote || pending) return;
    setPending(true);
    try {
      await cancelVote(planId, voteId);
      setVote(await getVote(planId, voteId));
    } finally {
      setPending(false);
    }
  }

  async function handleClose() {
    if (!vote || pending) return;
    setPending(true);
    try {
      setVote(await closeVote(planId, voteId));
    } finally {
      setPending(false);
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
  const isOpen = vote.status === "OPEN";
  // 투표를 만든 사람만 수정·마감·삭제할 수 있습니다. 서버도 같은 기준으로 막습니다.
  const isCreator = Boolean(user && user.id === vote.creatorId);

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title={vote.title} backHref={`/plans/${planId}/votes`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        {isOpen ? (
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
              disabled={!isOpen || Boolean(casting)}
              className="text-left disabled:cursor-default"
            >
              <VoteOptionBar label={opt.placeName} count={opt.voteCount} total={totalVotes} mine={opt.id === vote.myOptionId} />
            </button>
          ))}
        </div>

        {isOpen ? (
          <p className="text-center text-[12px] text-gray-500">후보를 눌러 투표하거나 표를 바꿀 수 있어요</p>
        ) : (
          <div className="rounded-2xl bg-gray-100 px-4 py-3 text-center text-[13px] font-bold text-gray-700">
            {vote.resultSummary ?? "투표 결과 확인 완료"}
          </div>
        )}

        {isOpen && myOption && (
          <Button variant="ghost" onClick={handleCancelVote} disabled={pending}>
            {pending ? "처리 중..." : "투표 취소하기"}
          </Button>
        )}

        {isCreator && (
          <div className="mt-2 flex flex-col gap-2 border-t border-gray-200 pt-4">
            <p className="text-[12px] text-gray-500">투표를 만든 사람만 할 수 있어요</p>
            {isOpen && (
              <>
                <Button href={`/plans/${planId}/votes/${voteId}/edit`} variant="soft">
                  투표 수정하기
                </Button>
                <Button variant="soft" onClick={handleClose} disabled={pending}>
                  {pending ? "마감하는 중..." : "지금 마감하기"}
                </Button>
              </>
            )}
            <Button href={`/plans/${planId}/votes/${voteId}/delete`} variant="ghost">
              투표 삭제하기
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
