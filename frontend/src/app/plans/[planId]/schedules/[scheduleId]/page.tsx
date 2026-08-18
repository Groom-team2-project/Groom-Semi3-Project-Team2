"use client";

import { use, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { CommentItem } from "@/components/plan/CommentItem";
import { getSchedule, getComments, addComment, deleteComment } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { USE_MOCK } from "@/lib/api/client";
import { store } from "@/lib/api/store";
import type { Comment, Schedule } from "@/lib/api";

export default function ScheduleDetailPage({
  params,
}: {
  params: Promise<{ planId: string; scheduleId: string }>;
}) {
  const { planId, scheduleId } = use(params);
  const searchParams = useSearchParams();
  const focusCommentId = searchParams.get("commentId");
  const [schedule, setSchedule] = useState<Schedule | null | undefined>(undefined);
  const [comments, setComments] = useState<Comment[]>([]);
  const [text, setText] = useState("");
  const [posting, setPosting] = useState(false);
  const [replyTo, setReplyTo] = useState<Comment | null>(null);
  const [isComposerOpen, setIsComposerOpen] = useState(false);
  const { user } = useAuth();
  const currentUserId = USE_MOCK ? store.me.id : user?.id;
  const commentsById = new Map(comments.map((comment) => [comment.id, comment]));
  const childComments = new Map<string, Comment[]>();
  const rootComments: Comment[] = [];

  comments.forEach((comment) => {
    if (!comment.parentCommentId || !commentsById.has(comment.parentCommentId)) {
      rootComments.push(comment);
      return;
    }

    const children = childComments.get(comment.parentCommentId) ?? [];
    children.push(comment);
    childComments.set(comment.parentCommentId, children);
  });

  useEffect(() => {
    getSchedule(planId, scheduleId).then(setSchedule);
    getComments(planId, scheduleId).then(setComments);
  }, [planId, scheduleId]);

  useEffect(() => {
    if (!focusCommentId || comments.length === 0) return;
    document.getElementById(`comment-${focusCommentId}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [comments, focusCommentId]);

  async function handlePost() {
    if (!text.trim() || text.length > 60 || posting) return;
    setPosting(true);
    try {
      const comment = await addComment(planId, scheduleId, text.trim(), replyTo?.id);
      setComments((prev) => [...prev, comment]);
      setText("");
      setReplyTo(null);
      setIsComposerOpen(false);
    } finally {
      setPosting(false);
    }
  }

  async function handleDelete(commentId: string) {
    await deleteComment(planId, scheduleId, commentId);
    setComments((prev) => prev.map((comment) => comment.id === commentId
      ? { ...comment, deleted: true, text: "삭제된 댓글입니다.", authorName: "삭제된 사용자" }
      : comment));
  }

  function openComposer(comment?: Comment) {
    setReplyTo(comment ?? null);
    setIsComposerOpen(true);
  }

  function closeComposer() {
    if (posting) return;
    setText("");
    setReplyTo(null);
    setIsComposerOpen(false);
  }

  function renderComments(items: Comment[], ancestors = new Set<string>(), showDividerAtEnd = true) {
    return items.map((comment, index) => {
      const children = childComments.get(comment.id) ?? [];
      const hasCycle = ancestors.has(comment.id);
      const isLastInGroup = index === items.length - 1;
      const showDivider = isLastInGroup && showDividerAtEnd;

      return (
        <div key={comment.id} id={`comment-${comment.id}`}>
          <CommentItem
            comment={comment}
            canDelete={comment.userId === currentUserId}
            showDivider={children.length === 0 && showDivider}
            onReply={() => openComposer(comment)}
            onDelete={() => handleDelete(comment.id)}
          />
          {!hasCycle && children.length > 0 && (
            <div className="bg-gray-100 pl-5 pr-2.5">
              {renderComments(children, new Set([...ancestors, comment.id]), showDivider)}
            </div>
          )}
        </div>
      );
    });
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
          <Button href={`/plans/${planId}/schedules/${scheduleId}/edit`} variant="ghost" size="sm" className="!bg-gray-100">
            수정
          </Button>
          <Button href={`/plans/${planId}/schedules/${scheduleId}/delete`} variant="danger" size="sm">
            삭제
          </Button>
        </div>

        <h3 className="mt-1 text-[13px] text-gray-500">댓글 {comments.length}</h3>
        <div>
          {rootComments.map((comment) => renderComments([comment]))}
        </div>
        <Button variant="soft" size="sm" onClick={() => openComposer()}>
          댓글 남기기
        </Button>
      </div>

      {isComposerOpen && (
        <div
          className="fixed inset-0 z-50 flex items-end bg-ink/40 sm:items-center sm:justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="comment-composer-title"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeComposer();
          }}
        >
          <div className="w-full rounded-t-2xl bg-white px-4 pb-6 pt-4 sm:max-w-md sm:rounded-2xl">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 id="comment-composer-title" className="text-[16px] font-bold text-ink">
                  {replyTo ? "답글 남기기" : "댓글 남기기"}
                </h2>
                {replyTo && <p className="mt-0.5 text-[12px] text-gray-500">{replyTo.authorName}님에게 답글을 남겨요</p>}
              </div>
              <button type="button" className="text-[13px] text-gray-500" onClick={closeComposer} disabled={posting}>닫기</button>
            </div>
            <textarea
              autoFocus
              className="min-h-28 w-full resize-none rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14.5px] text-ink placeholder:text-gray-500 focus:border-primary focus:bg-white focus:outline-none"
              placeholder="댓글을 남겨보세요"
              value={text}
              onChange={(event) => setText(event.target.value)}
            />
            <div className="mt-1.5 flex items-center justify-between">
              <p className="text-[12px] text-red">{text.length > 60 && "댓글은 60자까지 남길 수 있어요."}</p>
              <span className={`text-[12px] ${text.length > 60 ? "text-red" : "text-gray-500"}`}>{text.length} / 60</span>
            </div>
            <div className="mt-4 flex gap-2">
              <Button variant="ghost" size="sm" onClick={closeComposer} disabled={posting}>취소</Button>
              <Button size="sm" onClick={handlePost} disabled={!text.trim() || text.length > 60 || posting}>
                {posting ? "등록 중..." : "등록"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
