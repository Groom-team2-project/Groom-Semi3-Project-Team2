import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";

export function PlanNotFound() {
  return (
    <div className="flex min-h-dvh flex-col justify-center gap-4 px-6">
      <EmptyState emoji="🙈" title="계획을 찾을 수 없어요" description="삭제되었거나 잘못된 링크일 수 있어요" />
      <Button href="/plans" variant="ghost">
        내 여행 목록으로
      </Button>
    </div>
  );
}
