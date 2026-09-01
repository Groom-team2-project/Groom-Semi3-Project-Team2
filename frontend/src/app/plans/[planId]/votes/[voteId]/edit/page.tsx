"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput } from "@/components/ui/FieldInput";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchTrigger } from "@/components/plan/PlaceSearchTrigger";
import { consumePickedPlace } from "@/lib/pickedPlace";
import {
  getVote,
  updateVote,
  addVoteOption,
  updateVoteOption,
  deleteVoteOption,
} from "@/lib/api";
import type { Vote } from "@/lib/api";

/** ISO datetime -> datetime-local 입력값 */
function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function VoteEditPage({
  params,
}: {
  params: Promise<{ planId: string; voteId: string }>;
}) {
  const { planId, voteId } = use(params);
  const router = useRouter();

  const [vote, setVote] = useState<Vote | null | undefined>(undefined);
  const [title, setTitle] = useState("");
  const [deadline, setDeadline] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getVote(planId, voteId).then((found) => {
      setVote(found);
      if (found) {
        setTitle(found.title);
        setDeadline(toLocalInput(found.deadline));
      }
    });
  }, [planId, voteId]);

  // 장소 검색에서 돌아오면 고른 장소를 후보로 바로 추가합니다.
  useEffect(() => {
    const picked = consumePickedPlace();
    if (!picked) return;
    addVoteOption(planId, voteId, {
      placeName: picked.name,
      placeAddress: picked.address,
      emoji: picked.emoji,
    })
      .then(() => getVote(planId, voteId))
      .then(setVote)
      .catch(() => setError("후보를 추가하지 못했어요"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function run(action: () => Promise<unknown>, message: string) {
    setPending(true);
    setError(null);
    try {
      await action();
      setVote(await getVote(planId, voteId));
    } catch {
      setError(message);
    } finally {
      setPending(false);
    }
  }

  async function handleRename(optionId: string, current: string) {
    const next = window.prompt("후보 이름을 바꿉니다", current);
    if (!next || next.trim() === current) return;
    await run(
      () => updateVoteOption(planId, voteId, optionId, { placeName: next.trim() }),
      "후보를 수정하지 못했어요",
    );
  }

  async function handleRemove(optionId: string) {
    await run(
      () => deleteVoteOption(planId, voteId, optionId),
      "후보는 2개 아래로 줄일 수 없어요",
    );
  }

  async function handleSave() {
    setPending(true);
    setError(null);
    try {
      await updateVote(planId, voteId, {
        title: title.trim(),
        deadline: new Date(deadline).toISOString(),
      });
      router.push(`/plans/${planId}/votes/${voteId}`);
    } catch {
      setError("투표를 수정하지 못했어요. 마감 시간은 지금 이후여야 해요.");
      setPending(false);
    }
  }

  if (vote === undefined) return null;
  if (vote === null) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="투표 수정" backHref={`/plans/${planId}/votes`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">존재하지 않는 투표예요</p>
      </div>
    );
  }

  if (vote.status === "CLOSED") {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="투표 수정" backHref={`/plans/${planId}/votes/${voteId}`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">
          마감된 투표는 수정할 수 없어요
        </p>
      </div>
    );
  }

  const returnPath = encodeURIComponent(`/plans/${planId}/votes/${voteId}/edit`);
  const canSave = title.trim().length > 0 && deadline.length > 0 && !pending;

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="투표 수정" backHref={`/plans/${planId}/votes/${voteId}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="투표 제목">
          <FieldInput value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>

        <Field label="마감 시간">
          <FieldInput
            type="datetime-local"
            value={deadline}
            onChange={(e) => setDeadline(e.target.value)}
          />
        </Field>

        <Field label="후보 장소">
          <PlaceSearchTrigger
            href={`/plans/${planId}/places/search?return=${returnPath}&usage=vote_candidate`}
            label="카카오 장소 검색으로 후보 추가"
          />
          <div className="h-2" />
          <PlaceSearchTrigger
            href={`/plans/${planId}/places?return=${returnPath}`}
            label="저장된 장소 불러오기"
          />
        </Field>

        {vote.options.map((option) => (
          <div
            key={option.id}
            className="flex items-center gap-2 rounded-xl border border-gray-200 px-3 py-2.5"
          >
            <div className="min-w-0 flex-1">
              <PlaceRow
                emoji={option.emoji}
                name={option.placeName}
                address={option.placeAddress ?? `${option.voteCount}표`}
              />
            </div>
            <button
              type="button"
              onClick={() => handleRename(option.id, option.placeName)}
              disabled={pending}
              className="shrink-0 text-[12px] text-gray-500"
            >
              이름 변경
            </button>
            <button
              type="button"
              onClick={() => handleRemove(option.id)}
              disabled={pending}
              className="shrink-0 text-[12px] text-orange"
            >
              제거
            </button>
          </div>
        ))}

        <p className="text-[12px] text-gray-500">
          후보를 바꾸면 이미 투표한 멤버의 선택에 영향이 갈 수 있어요
        </p>

        {error && <p className="text-[12px] text-orange">{error}</p>}

        <div className="h-1" />
        <Button onClick={handleSave} disabled={!canSave}>
          {pending ? "저장하는 중..." : "저장하기"}
        </Button>
      </div>
    </div>
  );
}
