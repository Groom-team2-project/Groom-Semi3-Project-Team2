import { cx } from "@/lib/utils";
import type { Schedule } from "@/lib/api";

export function TimelineStop({ schedule, onClick }: { schedule: Schedule; onClick?: () => void }) {
  const isVote = Boolean(schedule.linkedVoteId);
  return (
    <button type="button" onClick={onClick} className="grid grid-cols-[52px_1fr] gap-2.5 text-left">
      <div className="pt-3.5 text-right font-mono text-[12px] font-bold text-primary">{schedule.time}</div>
      <div
        className={cx(
          "my-1 flex items-start gap-2 rounded-2xl border p-3.5",
          isVote ? "border-transparent bg-orange-soft" : "border-gray-200 bg-white",
        )}
      >
        <div className="min-w-0 flex-1">
          <div className="truncate text-[14.5px] font-bold">
            {schedule.emoji} {schedule.title ?? schedule.placeName}
          </div>

          <div className="truncate text-[12px] leading-relaxed text-gray-500">
            {schedule.placeAddress || "주소 정보 없음"}
          </div>
        </div>
        <div className="shrink-0 text-[14px] tracking-widest text-gray-300">⠿</div>
      </div>
    </button>
  );
}
