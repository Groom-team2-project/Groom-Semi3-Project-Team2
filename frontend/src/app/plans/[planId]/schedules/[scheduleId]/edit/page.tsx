"use client";

import { use, useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput, FieldTextarea } from "@/components/ui/FieldInput";
import { Toast } from "@/components/ui/Toast";
import { PlaceRow } from "@/components/plan/PlaceRow";
import { PlaceSearchTrigger } from "@/components/plan/PlaceSearchTrigger";
import { usePlan } from "@/lib/hooks/usePlan";
import { useFormDraft, hasDraft } from "@/lib/formDraft";
import { consumePickedPlace } from "@/lib/pickedPlace";
import { getScheduleLoadErrorMessage, getScheduleMutationErrorMessage } from "@/lib/scheduleError";
import { getSchedule, updateSchedule } from "@/lib/api";
import type { Schedule } from "@/lib/api";
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

const EMPTY_DRAFT: Draft = { day: 1, time: "09:00", placeId: "", placeName: "", placeAddress: "", emoji: "📍", memo: "" };

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
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [pending, setPending] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const closeToast = useCallback(() => setToast(null), []);

  useEffect(() => {
    let cancelled = false;

    getSchedule(planId, scheduleId)
      .then((schedule) => {
        if (cancelled) return;
        setOriginal(schedule);
        if (schedule && !hadDraftRef.current) {
          setDraft({ day: schedule.day, time: schedule.time, placeId: schedule.placeId ?? "", placeName: schedule.placeName, placeAddress: schedule.placeAddress ?? "", emoji: schedule.emoji, memo: schedule.memo ?? "" });
        }
      })
      .catch((error) => {
        if (cancelled) return;
        setLoadError(getScheduleLoadErrorMessage(error));
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planId, scheduleId, loadAttempt]);

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
  }, [planId, scheduleId]);

  function retryLoad() {
    setOriginal(undefined);
    setLoadError(null);
    setLoadAttempt((attempt) => attempt + 1);
  }

  if (loadError) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="일정 수정" backHref={`/plans/${planId}/timeline`} />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
          <p className="text-[13.5px] text-gray-700">{loadError}</p>
          <Button onClick={retryLoad} variant="ghost">
            다시 시도
          </Button>
        </div>
      </div>
    );
  }

  if (original === undefined || !plan) return null;
  if (original === null) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="일정 수정" backHref={`/plans/${planId}/timeline`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">삭제되었거나 존재하지 않는 일정이에요</p>
      </div>
    );
  }

  const planStartDate = plan.startDate;
  const dayCount = dateRangeToDayCount(planStartDate, plan.endDate);
  const returnPath = encodeURIComponent(`/plans/${planId}/schedules/${scheduleId}/edit`);
  const canSubmit = draft.placeId.length > 0 && draft.placeName.trim().length > 0 && Boolean(draft.time) && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      await updateSchedule(planId, scheduleId, {
        day: draft.day,
        date: dayIndexToDate(planStartDate, draft.day),
        time: draft.time,
        placeId: draft.placeId,
        placeName: draft.placeName,
        placeAddress: draft.placeAddress || undefined,
        emoji: draft.emoji,
        memo: draft.memo,
      });
      clearDraft();
      router.push(`/plans/${planId}/schedules/${scheduleId}`);
    } catch (error) {
      setToast(getScheduleMutationErrorMessage(error, "수정"));
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
                Day {day} · {formatDateShort(dayIndexToDate(planStartDate, day))}
              </option>
            ))}
          </select>
        </Field>

        <Field label="장소">
          <PlaceSearchTrigger
            href={`/plans/${planId}/places/search?return=${returnPath}&usage=schedule`}
            label="카카오 장소 검색으로 다시 찾기"
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
          {pending ? "저장하는 중..." : "수정 저장하기"}
        </Button>
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
