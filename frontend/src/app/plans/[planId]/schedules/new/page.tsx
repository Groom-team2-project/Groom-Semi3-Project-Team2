"use client";

import { use, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput, FieldTextarea } from "@/components/ui/FieldInput";
import { Toast } from "@/components/ui/Toast";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchTrigger } from "@/components/plan/PlaceSearchTrigger";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { useFormDraft } from "@/lib/formDraft";
import { consumePickedPlace } from "@/lib/pickedPlace";
import { getScheduleMutationErrorMessage } from "@/lib/scheduleError";
import { createSchedule } from "@/lib/api";
import { dateRangeToDayCount, dayIndexToDate, formatDateShort } from "@/lib/utils";

interface Draft {
  day: number;
  time: string;
  placeId: string;
  placeName: string;
  placeAddress: string;
  emoji: string;
  memo: string;
}

export default function ScheduleNewPage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ day?: string }>;
}) {
  const { planId } = use(params);
  const { day: dayParam } = use(searchParams);
  const router = useRouter();
  const { plan, isLoading } = usePlan(planId);
  const { draft, setDraft, clearDraft } = useFormDraft<Draft>(`tripmate_schedule_new_${planId}`, {
    day: Number(dayParam) || 1,
    time: "09:00",
    placeId: "",
    placeName: "",
    placeAddress: "",
    emoji: "📍",
    memo: "",
  });
  const [pending, setPending] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const closeToast = useCallback(() => setToast(null), []);

  useEffect(() => {
    const picked = consumePickedPlace();
    if (picked) {
      setDraft((draft) => ({
        ...draft,
        placeId: picked.id,
        placeName: picked.name,
        placeAddress: picked.address,
        emoji: picked.emoji,
      }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (isLoading) return null;
  if (!plan) return <PlanNotFound />;

  const planStartDate = plan.startDate;
  const dayCount = dateRangeToDayCount(planStartDate, plan.endDate);
  const returnPath = encodeURIComponent(`/plans/${planId}/schedules/new?day=${draft.day}`);
  const canSubmit = draft.placeId.length > 0 && draft.placeName.trim().length > 0 && Boolean(draft.time) && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      await createSchedule(planId, {
        day: draft.day,
        date: dayIndexToDate(planStartDate, draft.day),
        time: draft.time,
        placeId: draft.placeId,
        placeName: draft.placeName,
        placeAddress: draft.placeAddress || undefined,
        emoji: draft.emoji,
        memo: draft.memo || undefined,
      });
      clearDraft();
      router.push(`/plans/${planId}/timeline?day=${draft.day}`);
    } catch (error) {
      setToast(getScheduleMutationErrorMessage(error, "등록"));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="일정 추가" backHref={`/plans/${planId}/timeline?day=${draft.day}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="날짜">
          <select
            value={draft.day}
            onChange={(e) => setDraft((d) => ({ ...d, day: Number(e.target.value) }))}
            className="w-full rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14.5px] text-ink"
          >
            {Array.from({ length: dayCount }, (_, i) => i + 1).map((day) => (
              <option key={day} value={day}>
                Day {day} · {formatDateShort(dayIndexToDate(planStartDate, day))}
              </option>
            ))}
          </select>
        </Field>

        <Field label="장소">
          <PlaceSearchTrigger
            href={`/plans/${planId}/places/search?return=${returnPath}&usage=schedule`}
            label="카카오 장소 검색으로 추가하기"
          />
          <div className="h-2" />
          <PlaceSearchTrigger
            href={`/plans/${planId}/places?return=${returnPath}`}
            label="저장된 장소 불러오기"
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
          {pending ? "저장하는 중..." : "저장하기"}
        </Button>
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
