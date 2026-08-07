import { Card } from "@/components/ui/Card";
import { AvatarStack } from "@/components/ui/Avatar";
import { Tag } from "@/components/ui/Tag";
import { formatDateRange, formatDday } from "@/lib/utils";
import type { Plan } from "@/lib/api";

export function PlanCard({ plan }: { plan: Plan }) {
  const dday = formatDday(plan.startDate);
  const names = plan.members.map((m) => m.name);
  const colors = plan.members.map((m) => m.avatarColor);

  return (
    <Card href={`/plans/${plan.id}`}>
      {plan.emoji && (
        <div className="relative h-[88px] overflow-hidden rounded-xl bg-gradient-to-br from-primary to-[#6AA9FF]">
          {dday && (
            <em className="absolute right-2.5 top-2.5 rounded-full bg-white/90 px-2 py-1 text-[10.5px] font-bold not-italic text-ink">
              {dday}
            </em>
          )}
          <span className="absolute bottom-2.5 left-3 text-[15px] font-extrabold text-white">{plan.title}</span>
        </div>
      )}
      {!plan.emoji && (
        <div className="flex items-center gap-2">
          <span className="text-[15px] font-bold">{plan.title}</span>
          <div className="flex-1" />
          {plan.capacity && <Tag color="gray">정원 {plan.capacity}명</Tag>}
        </div>
      )}
      <div className="flex items-center gap-2.5">
        <div className="text-[12px] text-gray-500">
          {formatDateRange(plan.startDate, plan.endDate)} · 참여 {plan.members.length}명
        </div>
        <div className="flex-1" />
        <AvatarStack names={names} colors={colors} />
      </div>
    </Card>
  );
}
