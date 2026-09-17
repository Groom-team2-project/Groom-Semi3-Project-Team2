"use client";

import { use, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Toast } from "@/components/ui/Toast";
import { getSchedule, deleteSchedule } from "@/lib/api";
import { getScheduleMutationErrorMessage } from "@/lib/scheduleError";
import type { Schedule } from "@/lib/api";

export default function ScheduleDeleteConfirmPage({
  params,
}: {
  params: Promise<{ planId: string; scheduleId: string }>;
}) {
  const { planId, scheduleId } = use(params);
  const router = useRouter();
  const [schedule, setSchedule] = useState<Schedule | null>(null);
  const [pending, setPending] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const closeToast = useCallback(() => setToast(null), []);

  useEffect(() => {
    getSchedule(planId, scheduleId).then(setSchedule);
  }, [planId, scheduleId]);

  async function handleDelete() {
    setPending(true);
    try {
      await deleteSchedule(planId, scheduleId);
      router.push(`/plans/${planId}/timeline?day=${schedule?.day ?? 1}`);
    } catch (error) {
      setToast(getScheduleMutationErrorMessage(error, "삭제"));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="relative flex min-h-dvh flex-col">
      <AppBar title="일정 삭제" backHref={`/plans/${planId}/schedules/${scheduleId}`} />
      <div className="absolute inset-0 top-[52px] z-20 flex items-end bg-ink/45">
        <div className="flex w-full flex-col gap-3.5 rounded-t-[20px] bg-white px-5 pb-7 pt-5.5">
          <h3 className="text-[17px] font-extrabold">이 일정을 삭제할까요?</h3>
          <p className="text-[13.5px] leading-relaxed text-gray-700">
            &quot;{schedule?.title ?? schedule?.placeName ?? "이 일정"}&quot; 일정과 연결된 댓글도 함께 사라집니다. 이 작업은 되돌릴 수
            없어요.
          </p>
          <Button variant="danger" onClick={handleDelete} disabled={pending}>
            {pending ? "삭제하는 중..." : "일정 삭제하기"}
          </Button>
          <Button href={`/plans/${planId}/schedules/${scheduleId}`} variant="ghost">
            취소
          </Button>
        </div>
      </div>
      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}
