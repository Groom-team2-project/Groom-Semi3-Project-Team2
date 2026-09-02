import { formatRelativeTime } from "@/lib/utils";
import type { ActivityLog } from "@/lib/api";

export function ActivityRow({ activity, onClick }: { activity: ActivityLog; onClick?: () => void }) {
  // 삭제된 댓글처럼 이동할 대상이 없는 활동은 눌러도 토스트만 뜨므로, 목록에서 미리 흐리게 표시하고 클릭을 막음.
  const unavailable = activity.targetDeleted === true;
  // "댓글을 삭제했어요" 같은 요약은 이미 삭제를 알려주므로 안내 문구를 덧붙이지 않음.
  const deletedNotice = unavailable && activity.type !== "comment_deleted";

  const content = (
    <>
      <div
        className={`flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-full text-[13px] font-extrabold text-white ${unavailable ? "opacity-50" : ""}`}
        style={{ background: activity.actorColor }}
      >
        {activity.actorInitial}
      </div>
      <div className="min-w-0">
        <div className={`text-[13.5px] leading-relaxed ${unavailable ? "text-gray-500" : "text-ink"}`}>
          <b>{activity.actorName}</b>님이 {activity.summary}
        </div>
        <div className="mt-0.5 text-[11.5px] text-gray-500">
          {deletedNotice && "삭제된 댓글이에요 · "}
          {formatRelativeTime(activity.createdAt)}
        </div>
      </div>
    </>
  );

  if (unavailable) {
    return <div className="flex w-full items-start gap-2.5 border-b border-gray-200 py-2.5 text-left">{content}</div>;
  }

  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-start gap-2.5 border-b border-gray-200 py-2.5 text-left"
    >
      {content}
    </button>
  );
}
