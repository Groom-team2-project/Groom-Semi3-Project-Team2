"use client";

import { use, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { CommentItem } from "@/components/plan/CommentItem";
import { getSchedule, getComments, addComment, deleteComment, toggleCommentLike } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { ApiError, USE_MOCK } from "@/lib/api/client";
import { store } from "@/lib/api/store";
import type { Comment, Schedule } from "@/lib/api";

const RESERVATION_LABEL = {
  NOT_REQUIRED: "예약 불필요",
  UNRESERVED: "예약 전",
  RESERVED: "예약 완료",
  CANCELLED: "예약 취소",
} as const;

function getCommentPostError(error: unknown): string {
  if (!(error instanceof ApiError)) return "댓글 등록에 실패했어요. 다시 시도해 주세요.";

  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return "댓글 작성 권한이 없어요.";
  if (error.status === 404) return "일정이 삭제되었거나 더 이상 볼 수 없어요.";
  if (error.status === 400) return "일정 또는 답글 대상이 더 이상 유효하지 않아요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";

  return "댓글 등록에 실패했어요. 다시 시도해 주세요.";
}

function getCommentLoadError(error: unknown): string {
  if (!(error instanceof ApiError)) return "댓글을 불러오지 못했어요. 다시 시도해 주세요.";

  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return "댓글을 볼 수 있는 권한이 없어요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";

  return "댓글을 불러오지 못했어요. 다시 시도해 주세요.";
}

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
  const [postError, setPostError] = useState("");
  const [replyTo, setReplyTo] = useState<Comment | null>(null);
  const [isComposerOpen, setIsComposerOpen] = useState(false);
  const [commentsError, setCommentsError] = useState("");
  const [likePendingIds, setLikePendingIds] = useState<Set<string>>(new Set());
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
    getSchedule(planId, scheduleId)
      .then(setSchedule)
      .catch(() => setSchedule(null));
    getComments(planId, scheduleId)
      .then(setComments)
      .catch((error) => setCommentsError(getCommentLoadError(error)));
  }, [planId, scheduleId]);

  useEffect(() => {
    if (!focusCommentId || comments.length === 0) return;
    document.getElementById(`comment-${focusCommentId}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [comments, focusCommentId]);

  async function handlePost() {
    if (!text.trim() || text.length > 60 || posting) return;
    setPosting(true);
    setPostError("");
    try {
      const comment = await addComment(planId, scheduleId, text.trim(), replyTo?.id);
      setComments((prev) => [...prev, comment]);
      setText("");
      setReplyTo(null);
      setIsComposerOpen(false);
    } catch (error) {
      setPostError(getCommentPostError(error));
    } finally {
      setPosting(false);
    }
  }

  async function handleLike(commentId: string) {
    // 응답 역전으로 최신 상태를 덮어쓰지 않도록, 진행 중인 요청은 무시
    if (likePendingIds.has(commentId)) return;

    const target = comments.find((comment) => comment.id === commentId);
    if (!target) return;
    const previousLikedByMe = target.likedByMe;
    const previousLikeCount = target.likeCount;

    setLikePendingIds((prev) => new Set(prev).add(commentId));
    // 낙관적 업데이트, 실패 시 이 댓글만 원상 복구
    setComments((prev) => prev.map((comment) => comment.id === commentId
      ? { ...comment, likedByMe: !previousLikedByMe, likeCount: previousLikeCount + (previousLikedByMe ? -1 : 1) }
      : comment));
    try {
      const { likeCount, likedByMe } = await toggleCommentLike(planId, scheduleId, commentId);
      setComments((prev) => prev.map((comment) => comment.id === commentId
        ? { ...comment, likeCount, likedByMe }
        : comment));
    } catch {
      setComments((prev) => prev.map((comment) => comment.id === commentId
        ? { ...comment, likedByMe: previousLikedByMe, likeCount: previousLikeCount }
        : comment));
    } finally {
      setLikePendingIds((prev) => {
        const next = new Set(prev);
        next.delete(commentId);
        return next;
      });
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
    setPostError("");
    setIsComposerOpen(true);
  }

  function closeComposer() {
    if (posting) return;
    setText("");
    setReplyTo(null);
    setPostError("");
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
            onLike={() => handleLike(comment.id)}
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
      <AppBar title={`${schedule.emoji} ${schedule.title ?? schedule.placeName}`} backHref={`/plans/${planId}/timeline?day=${schedule.day}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <div className="text-[12.5px] text-gray-500">
          📍 {schedule.placeId ? `${schedule.placeName} · ${schedule.placeAddress ?? "주소 정보 없음"}` : "장소 정보 없음"}
        </div>
        <div className="text-[12.5px] text-gray-500">
          🕐 {schedule.time}{schedule.endAt ? ` ~ ${schedule.endAt.slice(11, 16)}` : ""}
          {schedule.reservationStatus ? ` · ${RESERVATION_LABEL[schedule.reservationStatus]}` : ""}
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
        {commentsError && <p className="text-[12.5px] text-red">{commentsError}</p>}
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
            {postError && <p role="alert" className="mt-1.5 text-[12px] text-red">{postError}</p>}
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
