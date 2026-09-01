"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput } from "@/components/ui/FieldInput";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchTrigger } from "@/components/plan/PlaceSearchTrigger";
import { useFormDraft } from "@/lib/formDraft";
import { consumePickedPlace } from "@/lib/pickedPlace";
import { createVote } from "@/lib/api";

interface Candidate {
  name: string;
  address: string;
  emoji: string;
}

interface Draft {
  title: string;
  candidates: Candidate[];
  deadline: string; // datetime-local
}

function defaultDeadline() {
  const d = new Date(Date.now() + 3 * 3_600_000);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function VoteCreatePage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const router = useRouter();
  const { draft, setDraft, clearDraft } = useFormDraft<Draft>(`tripmate_vote_new_${planId}`, {
    title: "",
    candidates: [],
    deadline: defaultDeadline(),
  });
  const [pending, setPending] = useState(false);

  useEffect(() => {
    const picked = consumePickedPlace();
    if (picked) {
      setDraft((d) =>
        d.candidates.some((c) => c.name === picked.name)
          ? d
          : { ...d, candidates: [...d.candidates, { name: picked.name, address: picked.address, emoji: picked.emoji }] },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const returnPath = encodeURIComponent(`/plans/${planId}/votes/new`);
  const canSubmit = draft.title.trim().length > 0 && draft.candidates.length >= 2 && !pending;

  function removeCandidate(index: number) {
    setDraft((d) => ({ ...d, candidates: d.candidates.filter((_, i) => i !== index) }));
  }

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      const vote = await createVote(planId, {
        title: draft.title.trim(),
        deadline: new Date(draft.deadline).toISOString(),
        options: draft.candidates.map((c) => ({ placeName: c.name, placeAddress: c.address, emoji: c.emoji })),
      });
      clearDraft();
      router.push(`/plans/${planId}/votes/${vote.id}`);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="투표 만들기" backHref={`/plans/${planId}/votes`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="투표 제목">
          <FieldInput
            placeholder="ex) 둘째날 저녁 뭐 먹지?"
            value={draft.title}
            onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
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

        {draft.candidates.map((c, i) => (
          <div key={i} className="flex items-center gap-2 rounded-xl border border-gray-200 px-3 py-2.5">
            <div className="min-w-0 flex-1">
              <PlaceRow emoji={c.emoji} name={c.name} address={`후보 ${i + 1}`} />
            </div>
            <button type="button" onClick={() => removeCandidate(i)} className="shrink-0 text-[12px] text-gray-500">
              제거
            </button>
          </div>
        ))}
        {draft.candidates.length > 0 && draft.candidates.length < 2 && (
          <p className="text-[12px] text-orange">후보는 최소 2개 이상 추가해주세요</p>
        )}

        <Field label="마감 시간">
          <FieldInput type="datetime-local" value={draft.deadline} onChange={(e) => setDraft((d) => ({ ...d, deadline: e.target.value }))} />
        </Field>

        <div className="h-1" />
        <Button onClick={handleSubmit} disabled={!canSubmit}>
          {pending ? "만드는 중..." : "투표 시작하기"}
        </Button>
      </div>
    </div>
  );
}
