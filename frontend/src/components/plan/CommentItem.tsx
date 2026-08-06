import { formatRelativeTime } from "@/lib/utils";
import type { Comment } from "@/lib/api";

export function CommentItem({ comment }: { comment: Comment }) {
  return (
    <div className="flex gap-2 border-b border-gray-200 py-2.5">
      <div className="h-[26px] w-[26px] shrink-0 rounded-full" style={{ background: comment.authorColor }} />
      <div className="min-w-0">
        <div className="flex items-baseline gap-1.5">
          <span className="text-[12px] font-bold">{comment.authorName}</span>
          <span className="text-[10.5px] text-gray-500">{formatRelativeTime(comment.createdAt)}</span>
        </div>
        <div className="mt-0.5 text-[13px] text-gray-700">{comment.text}</div>
      </div>
    </div>
  );
}
