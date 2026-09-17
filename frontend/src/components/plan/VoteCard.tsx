import { cx } from "@/lib/utils";
import { Tag } from "@/components/ui/Tag";
import type { Vote } from "@/lib/api";

export function VoteOptionBar({ label, count, total, mine }: { label: string; count: number; total: number; mine?: boolean }) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div className={cx("flex flex-col gap-2 rounded-2xl border p-3.5", mine ? "border-[1.5px] border-primary" : "border-[1.5px] border-gray-200")}>
      <div className="flex items-center justify-between">
        <span className="text-[14.5px] font-bold">
          {mine && "✅ "}
          {label}
        </span>
        <span className="text-[12px] font-bold text-gray-700">
          {count}표 ({pct}%)
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-gray-100">
        <div className="h-full rounded-full bg-primary" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function VoteListCard({ vote, onClick }: { vote: Vote; onClick?: () => void }) {
  const totalVotes = vote.options.reduce((sum, o) => sum + o.voteCount, 0);
  const isMine = Boolean(vote.myOptionId);

  const body = (
    <>
      <div className="flex items-center justify-between gap-2">
        <span className="text-[14.5px] font-bold">{vote.title}</span>
        {vote.status === "OPEN" ? (
          <Tag color="orange">투표중</Tag>
        ) : (
          <Tag color="gray">마감 · 일정 반영됨</Tag>
        )}
      </div>
      <div className="text-[12px] font-bold text-gray-700">
        {vote.status === "OPEN"
          ? isMine
            ? `투표 완료 · 총 ${totalVotes}표`
            : `아직 투표하지 않았어요 · 총 ${totalVotes}표`
          : vote.resultSummary}
      </div>
    </>
  );

  return (
    <button
      type="button"
      onClick={onClick}
      className={cx(
        "flex w-full flex-col gap-2 rounded-2xl border-[1.5px] bg-white p-4 text-left",
        isMine && vote.status === "OPEN" ? "border-primary" : "border-gray-200",
      )}
    >
      {body}
    </button>
  );
}
