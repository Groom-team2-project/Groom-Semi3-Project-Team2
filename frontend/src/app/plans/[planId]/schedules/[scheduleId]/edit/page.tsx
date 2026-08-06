"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput, FieldTextarea } from "@/components/ui/FieldInput";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchTrigger } from "@/components/plan/PlaceSearchTrigger";
import { usePlan } from "@/lib/hooks/usePlan";
import { useFormDraft, hasDraft } from "@/lib/formDraft";
import { consumePickedPlace } from "@/lib/pickedPlace";
import { getSchedule, updateSchedule } from "@/lib/api";
import type { Schedule } from "@/lib/api";
import { dateRangeToDayCount, dayIndexToDate, formatDateShort } from "@/lib/utils";

interface Draft {
  day: number;
  time: string;
  placeName: string;
  placeAddress: string;
  emoji: string;
  memo: string;
}

const EMPTY_DRAFT: Draft = { day: 1, time: "09:00", placeName: "", placeAddress: "", emoji: "📍", memo: "" };

export default function ScheduleEditPage({
  params,
}: {
  params: Promise<{ planId: string; scheduleId: string }>;
}) {
  const { planId, scheduleId } = use(params);
  const router = useRouter();
  const { plan } = usePlan(planId);
  const draftKey = `tripmate_schedule_edit_${scheduleId}`;
  const hadDraftRef = useRef(hasDraft(draftKey));
  const { draft, setDraft, clearDraft } = useFormDraft<Draft>(draftKey, EMPTY_DRAFT);
  const [original, setOriginal] = useState<Schedule | null | undefined>(undefined);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    getSchedule(planId, scheduleId).then((s) => {
      setOriginal(s);
      if (s && !hadDraftRef.current) {
        setDraft({ day: s.day, time: s.time, placeName: s.placeName, placeAddress: s.placeAddress ?? "", emoji: s.emoji, memo: s.memo ?? "" });
      }
    });
    const picked = consumePickedPlace();
    if (picked) {
      setDraft((d) => ({ ...d, placeName: picked.name, placeAddress: picked.address, emoji: picked.emoji }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planId, scheduleId]);

  if (original === undefined || !plan) return null;
  if (original === null) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="일정 수정" backHref={`/plans/${planId}/timeline`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">삭제되었거나 존재하지 않는 일정이에요</p>
      </div>
    );
  }

  const dayCount = dateRangeToDayCount(plan.startDate, plan.endDate);
  const returnPath = encodeURIComponent(`/plans/${planId}/schedules/${scheduleId}/edit`);
  const canSubmit = draft.placeName.trim().length > 0 && draft.time && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      await updateSchedule(planId, scheduleId, {
        day: draft.day,
        time: draft.time,
        placeName: draft.placeName,
        placeAddress: draft.placeAddress || undefined,
        emoji: draft.emoji,
        memo: draft.memo || undefined,
      });
      clearDraft();
      router.push(`/plans/${planId}/schedules/${scheduleId}`);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="일정 수정" backHref={`/plans/${planId}/schedules/${scheduleId}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="날짜">
          <select
            value={draft.day}
            onChange={(e) => setDraft((d) => ({ ...d, day: Number(e.target.value) }))}
            className="w-full rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14.5px] text-ink"
          >
            {Array.from({ length: dayCount }, (_, i) => i + 1).map((day) => (
              <option key={day} value={day}>
                Day {day} · {formatDateShort(dayIndexToDate(plan.startDate, day))}
              </option>
            ))}
          </select>
        </Field>

        <Field label="장소">
          <PlaceSearchTrigger
            href={`/plans/${planId}/places/search?return=${returnPath}&usage=schedule`}
            label="카카오 장소 검색으로 다시 찾기"
          />
        </Field>

        {draft.placeName && (
          <Field label="선택된 장소">
            <PlaceRow emoji={draft.emoji} name={draft.placeName} address={draft.placeAddress} />
          </Field>
        )}

        <Field label="시간">
          <FieldInput type="time" value={draft.time} onChange={(e) => setDraft((d) => ({ ...d, time: e.target.value }))} />
        </Field>

        <Field label="메모">
          <FieldTextarea
            placeholder="예약 필요, 웨이팅 있음 등"
            value={draft.memo}
            onChange={(e) => setDraft((d) => ({ ...d, memo: e.target.value }))}
          />
        </Field>

        <div className="h-1" />
        <Button onClick={handleSubmit} disabled={!canSubmit}>
          {pending ? "저장하는 중..." : "수정 저장하기"}
        </Button>
      </div>
    </div>
  );
}
