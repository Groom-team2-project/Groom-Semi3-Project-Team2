import { Tag } from "@/components/ui/Tag";
import { cx } from "@/lib/utils";
import type { ReservationStatus, Schedule } from "@/lib/api";

const RESERVATION_LABEL: Record<ReservationStatus, string> = {
  NOT_REQUIRED: "예약 불필요",
  UNRESERVED: "예약 전",
  RESERVED: "예약 완료",
  CANCELLED: "예약 취소",
};

export function TimelineStop({
  schedule,
  onSelect,
  isDragging = false,
  isDraggable = false,
}: {
  schedule: Schedule;
  onSelect?: () => void;
  isDragging?: boolean;
  isDraggable?: boolean;
}) {
  const isVote = Boolean(schedule.linkedVoteId);

  return (
    <button
      type="button"
      onClick={onSelect}
      className={cx(
        "grid w-full grid-cols-[52px_1fr] gap-2.5 text-left transition-opacity",
        isDraggable && "cursor-grab active:cursor-grabbing",
        isDragging && "opacity-40",
      )}
    >
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
            {schedule.placeId ? schedule.placeName : "연결된 장소 없음"}
          </div>

          {(schedule.memo || schedule.reservationStatus) && (
            <div className="mt-2 flex flex-wrap items-center gap-1.5">
              {schedule.reservationStatus && (
                <Tag color={schedule.reservationStatus === "UNRESERVED" ? "orange" : "blue"}>
                  {RESERVATION_LABEL[schedule.reservationStatus]}
                </Tag>
              )}
              {schedule.memo && (
                <span className="min-w-0 flex-1 truncate text-[11.5px] text-gray-700">메모 · {schedule.memo}</span>
              )}
            </div>
          )}
        </div>
      </div>
    </button>
  );
}
