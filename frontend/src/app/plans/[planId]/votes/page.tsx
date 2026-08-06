"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { BottomTabBar } from "@/components/ui/BottomTabBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { VoteListCard } from "@/components/plan/VoteCard";
import { useRememberPlan } from "@/lib/lastPlan";
import { getVotes } from "@/lib/api";
import type { Vote } from "@/lib/api";

export default function VoteListPage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const router = useRouter();
  const [votes, setVotes] = useState<Vote[] | null>(null);

  useRememberPlan(planId);

  useEffect(() => {
    getVotes(planId).then(setVotes);
  }, [planId]);

  const openVotes = votes?.filter((v) => v.status === "OPEN") ?? [];
  const closedVotes = votes?.filter((v) => v.status === "CLOSED") ?? [];

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar
        title="투표"
        actions={
          <Button href={`/plans/${planId}/votes/new`} size="sm" fullWidth={false}>
            + 투표 만들기
          </Button>
        }
      />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        {votes?.length === 0 && (
          <EmptyState emoji="🗳️" title="아직 투표가 없어요" description="의견이 갈리는 장소가 있다면 투표를 만들어보세요" />
        )}
        {openVotes.length > 0 && (
          <>
            <h3 className="text-[13px] text-gray-500">진행중</h3>
            {openVotes.map((v) => (
              <VoteListCard key={v.id} vote={v} onClick={() => router.push(`/plans/${planId}/votes/${v.id}`)} />
            ))}
          </>
        )}
        {closedVotes.length > 0 && (
          <>
            <h3 className="mt-2 text-[13px] text-gray-500">마감됨</h3>
            {closedVotes.map((v) => (
              <VoteListCard key={v.id} vote={v} onClick={() => router.push(`/plans/${planId}/votes/${v.id}`)} />
            ))}
          </>
        )}
      </div>
      <BottomTabBar planId={planId} />
    </div>
  );
}
