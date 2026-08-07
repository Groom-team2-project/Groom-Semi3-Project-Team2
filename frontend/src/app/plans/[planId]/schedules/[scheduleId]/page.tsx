"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { FieldInput } from "@/components/ui/FieldInput";
import { CommentItem } from "@/components/plan/CommentItem";
import { getSchedule, getComments, addComment } from "@/lib/api";
import type { Comment, Schedule } from "@/lib/api";

export default function ScheduleDetailPage({
  params,
}: {
  params: Promise<{ planId: string; scheduleId: string }>;
}) {
  const { planId, scheduleId } = use(params);
  const [schedule, setSchedule] = useState<Schedule | null | undefined>(undefined);
  const [comments, setComments] = useState<Comment[]>([]);
  const [text, setText] = useState("");
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    getSchedule(planId, scheduleId).then(setSchedule);
    getComments(scheduleId).then(setComments);
  }, [planId, scheduleId]);

  async function handlePost() {
    if (!text.trim() || posting) return;
    setPosting(true);
    try {
      const comment = await addComment(planId, scheduleId, text.trim());
      setComments((prev) => [...prev, comment]);
      setText("");
    } finally {
      setPosting(false);
    }
  }

  if (schedule === undefined) return null;
  if (schedule === null) {
    return (
      <div className="flex min-h-dvh flex-col">
        <AppBar title="일정" backHref={`/plans/${planId}/timeline`} />
        <p className="px-4 pt-8 text-center text-[13px] text-gray-500">삭제되었거나 존재하지 않는 일정이에요</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title={`${schedule.emoji} ${schedule.placeName}`} backHref={`/plans/${planId}/timeline?day=${schedule.day}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <div className="text-[12.5px] text-gray-500">
          📍 {schedule.placeAddress ?? "장소 정보 없음"} · {schedule.time}
        </div>
        {schedule.memo && <div className="rounded-xl bg-gray-100 px-3.5 py-3 text-[13.5px] text-gray-700">{schedule.memo}</div>}

        <div className="flex gap-2">
          <Button href={`/plans/${planId}/schedules/${scheduleId}/edit`} variant="ghost" size="sm">
            수정
          </Button>
          <Button href={`/plans/${planId}/schedules/${scheduleId}/delete`} variant="danger" size="sm">
            삭제
          </Button>
        </div>

        <h3 className="mt-1 text-[13px] text-gray-500">댓글 {comments.length}</h3>
        <div>
          {comments.map((c) => (
            <CommentItem key={c.id} comment={c} />
          ))}
        </div>
        <div className="mt-1 flex gap-2">
          <FieldInput
            placeholder="댓글 남기기..."
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handlePost()}
          />
          <Button size="sm" fullWidth={false} onClick={handlePost} disabled={!text.trim() || posting}>
            등록
          </Button>
        </div>
      </div>
    </div>
  );
}
