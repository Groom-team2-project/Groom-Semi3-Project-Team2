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
import type { ReservationStatus } from "@/lib/api";
import { dateRangeToDayCount, dayIndexToDate, formatDateShort } from "@/lib/utils";

interface Draft {
  day: number;
  title: string;
  startTime: string;
  endTime: string;
  reservationStatus: ReservationStatus;
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
    title: "",
    startTime: "09:00",
    endTime: "",
    reservationStatus: "NOT_REQUIRED",
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
  const canSubmit = draft.title.trim().length > 0 && Boolean(draft.startTime) && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      const date = dayIndexToDate(planStartDate, draft.day);
      await createSchedule(planId, {
        title: draft.title.trim(),
        startAt: `${date}T${draft.startTime}:00`,
        endAt: draft.endTime ? `${date}T${draft.endTime}:00` : undefined,
        reservationStatus: draft.reservationStatus,
        placeId: draft.placeId || undefined,
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
        <Field label="일정 제목">
          <FieldInput
            maxLength={200}
            placeholder="예: 성산일출봉 일출 관람"
            value={draft.title}
            onChange={(event) => setDraft((current) => ({ ...current, title: event.target.value }))}
          />
        </Field>

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

        <Field label="장소" optional>
          <PlaceSearchTrigger
            href={`/plans/${planId}/places/search?return=${returnPath}&usage=schedule`}
            label="카카오 장소 검색으로 추가하기"
          />
        </Field>

        {draft.placeName && (
          <Field label="선택된 장소">
            <PlaceRow emoji={draft.emoji} name={draft.placeName} address={draft.placeAddress} />
          </Field>
        )}

        <Field label="시작 시간">
          <FieldInput
            type="time"
            value={draft.startTime}
            onChange={(event) => setDraft((current) => ({ ...current, startTime: event.target.value }))}
          />
        </Field>

        <Field label="종료 시간" optional>
          <FieldInput
            type="time"
            value={draft.endTime}
            onChange={(event) => setDraft((current) => ({ ...current, endTime: event.target.value }))}
          />
        </Field>

        <Field label="예약 상태">
          <select
            value={draft.reservationStatus}
            onChange={(event) => setDraft((current) => ({
              ...current,
              reservationStatus: event.target.value as ReservationStatus,
            }))}
            className="w-full rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14.5px] text-ink"
          >
            <option value="NOT_REQUIRED">예약 불필요</option>
            <option value="UNRESERVED">예약 전</option>
            <option value="RESERVED">예약 완료</option>
            <option value="CANCELLED">예약 취소</option>
          </select>
        </Field>

        <Field label="메모" optional>
          <FieldTextarea
            maxLength={1000}
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
