import { formatRelativeTime } from "@/lib/utils";
import type { ActivityLog } from "@/lib/api";

export function ActivityRow({ activity, onClick }: { activity: ActivityLog; onClick?: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-start gap-2.5 border-b border-gray-200 py-2.5 text-left"
    >
      <div
        className="flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-full text-[13px] font-extrabold text-white"
        style={{ background: activity.actorColor }}
      >
        {activity.actorInitial}
      </div>
      <div className="min-w-0">
        <div className="text-[13.5px] leading-relaxed text-ink">
          <b>{activity.actorName}</b>님이 {activity.summary}
        </div>
        <div className="mt-0.5 text-[11.5px] text-gray-500">{formatRelativeTime(activity.createdAt)}</div>
      </div>
    </button>
  );
}
