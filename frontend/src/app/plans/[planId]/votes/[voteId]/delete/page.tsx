"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { getVote, deleteVote } from "@/lib/api";
import type { Vote } from "@/lib/api";

export default function VoteDeleteConfirmPage({
  params,
}: {
  params: Promise<{ planId: string; voteId: string }>;
}) {
  const { planId, voteId } = use(params);
  const router = useRouter();
  const [vote, setVote] = useState<Vote | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    getVote(planId, voteId).then(setVote);
  }, [planId, voteId]);

  async function handleDelete() {
    setPending(true);
    try {
      await deleteVote(planId, voteId);
      router.push(`/plans/${planId}/votes`);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="relative flex min-h-dvh flex-col">
      <AppBar title="투표 삭제" backHref={`/plans/${planId}/votes/${voteId}`} />
      <div className="absolute inset-0 top-[52px] z-20 flex items-end bg-ink/45">
        <div className="flex w-full flex-col gap-3.5 rounded-t-[20px] bg-white px-5 pb-7 pt-5.5">
          <h3 className="text-[17px] font-extrabold">이 투표를 삭제할까요?</h3>
          <p className="text-[13.5px] leading-relaxed text-gray-700">
            &quot;{vote?.title ?? "이 투표"}&quot;의 후보와 지금까지 모인 표가 함께 사라집니다. 이 작업은 되돌릴 수
            없어요.
          </p>
          <Button variant="danger" onClick={handleDelete} disabled={pending}>
            {pending ? "삭제하는 중..." : "투표 삭제하기"}
          </Button>
          <Button href={`/plans/${planId}/votes/${voteId}`} variant="ghost">
            취소
          </Button>
        </div>
      </div>
    </div>
  );
}
