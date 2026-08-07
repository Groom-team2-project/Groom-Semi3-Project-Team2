"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState } from "@/components/ui/EmptyState";
import { PlanCard } from "@/components/plan/PlanCard";
import { useAuth } from "@/context/AuthContext";
import { getPlans } from "@/lib/api";
import type { Plan } from "@/lib/api";
import Link from "next/link";

export default function PlanListPage() {
  const { user, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const [plans, setPlans] = useState<Plan[] | null>(null);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    getPlans().then(setPlans);
  }, [user]);

  if (authLoading || !user) return null;

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar
        title="내 여행"
        subtitle={`${user.name} 님 · 계획 ${plans?.length ?? 0}개`}
        actions={
          <>
            <Button href="/plans/new" size="sm" fullWidth={false}>
              + 새 계획
            </Button>
            <Link href="/profile" aria-label="프로필">
              <Avatar name={user.name} color={user.avatarColor} size="md" />
            </Link>
          </>
        }
      />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        {plans === null && <div className="pt-10 text-center text-[13px] text-gray-500">불러오는 중...</div>}
        {plans?.length === 0 && (
          <EmptyState title="아직 여행 계획이 없어요" description="새 계획을 만들고 친구들을 초대해보세요" emoji="🧳" />
        )}
        {plans?.map((plan) => (
          <PlanCard key={plan.id} plan={plan} />
        ))}
      </div>
    </div>
  );
}
