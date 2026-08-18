import { formatRelativeTime } from "@/lib/utils";
import type { Comment } from "@/lib/api";

interface CommentItemProps {
  comment: Comment;
  canDelete?: boolean;
  showDivider?: boolean;
  onReply?: () => void;
  onDelete?: () => void;
}

export function CommentItem({
  comment,
  canDelete = false,
  showDivider = false,
  onReply,
  onDelete,
}: CommentItemProps) {
  return (
    <div className={`${showDivider ? "border-b border-gray-200" : ""} flex gap-2 py-2.5`}>
      <div className="h-[26px] w-[26px] shrink-0 rounded-full" style={{ background: comment.authorColor }} />
      <div className="min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-[12px] font-bold">{comment.deleted ? "삭제된 사용자" : comment.authorName}</span>
          <span className="text-[10.5px] text-gray-500">{formatRelativeTime(comment.createdAt)}</span>
          {!comment.deleted && (
            <div className="ml-auto flex items-center gap-2 text-[11px]">
              <button type="button" className="text-primary" onClick={onReply}>답글</button>
              {canDelete && <button type="button" className="text-red" onClick={onDelete}>삭제</button>}
            </div>
          )}
        </div>
        <div className="mt-0.5 text-[13px] text-gray-700">{comment.text}</div>
      </div>
    </div>
  );
}
