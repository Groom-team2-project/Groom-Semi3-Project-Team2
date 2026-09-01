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
import type { ReservationStatus, Schedule } from "@/lib/api";
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

const EMPTY_DRAFT: Draft = {
  day: 1,
  title: "",
  startTime: "09:00",
  endTime: "",
  reservationStatus: "NOT_REQUIRED",
  placeId: "",
  placeName: "",
  placeAddress: "",
  emoji: "📍",
  memo: "",
};

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
          setDraft({
            day: schedule.day,
            title: schedule.title ?? schedule.placeName,
            startTime: schedule.time,
            endTime: schedule.endAt?.slice(11, 16) ?? "",
            reservationStatus: schedule.reservationStatus ?? "NOT_REQUIRED",
            placeId: schedule.placeId ?? "",
            placeName: schedule.placeId ? schedule.placeName : "",
            placeAddress: schedule.placeAddress ?? "",
            emoji: schedule.emoji,
            memo: schedule.memo ?? "",
          });
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
  const canSubmit = draft.title.trim().length > 0 && Boolean(draft.startTime) && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    if (draft.endTime && draft.endTime < draft.startTime) {
      setToast("종료 시간은 시작 시간과 같거나 늦어야 해요.");
      return;
    }
    setPending(true);
    try {
      const date = dayIndexToDate(planStartDate, draft.day);
      await updateSchedule(planId, scheduleId, {
        title: draft.title.trim(),
        startAt: `${date}T${draft.startTime}:00`,
        endAt: draft.endTime ? `${date}T${draft.endTime}:00` : undefined,
        reservationStatus: draft.reservationStatus,
        placeId: draft.placeId || undefined,
        memo: draft.memo || undefined,
        clearPlace: Boolean(original.placeId) && !draft.placeId,
        clearMemo: Boolean(original.memo) && !draft.memo,
        clearEndAt: Boolean(original.endAt) && !draft.endTime,
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
        <Field label="일정 제목">
          <FieldInput
            maxLength={200}
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
            <Button
              variant="danger"
              size="sm"
              fullWidth={false}
              className="mt-2"
              onClick={() => setDraft((current) => ({
                ...current,
                placeId: "",
                placeName: "",
                placeAddress: "",
                emoji: "📍",
              }))}
            >
              장소 제거
            </Button>
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
          {pending ? "저장하는 중..." : "수정 저장하기"}
        </Button>
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
