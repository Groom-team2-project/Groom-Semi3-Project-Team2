"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import { Field, FieldInput } from "@/components/ui/FieldInput";
import { BottomTabBar } from "@/components/ui/BottomTabBar";
import { useAuth } from "@/context/AuthContext";
import { useLastPlanId } from "@/lib/lastPlan";
import { getPlans } from "@/lib/api";
import type { Plan, Role } from "@/lib/api";

const ROLE_LABEL: Record<Role, string> = { OWNER: "모임장", EDITOR: "편집자", VIEWER: "뷰어" };

export default function ProfilePage() {
  const { user, isLoading, updateProfile, logout } = useAuth();
  const router = useRouter();
  const lastPlanId = useLastPlanId();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [nickname, setNickname] = useState("");
  const [syncedUser, setSyncedUser] = useState(user);

  useEffect(() => {
    if (!isLoading && !user) router.replace("/login");
  }, [isLoading, user, router]);

  // user는 마운트 후 비동기로 채워짐 — 처음 채워졌을 때만 입력값 초기화
  // (effect가 아니라 렌더 중 상태 조정 패턴: https://react.dev/learn/you-might-not-need-an-effect)
  if (user && user !== syncedUser) {
    setSyncedUser(user);
    setNickname(user.name);
  }

  useEffect(() => {
    getPlans().then(setPlans);
  }, []);

  async function handleLogout() {
    await logout();
    router.replace("/login");
  }

  async function handleNicknameBlur() {
    if (!user || !nickname.trim() || nickname === user.name) return;
    await updateProfile(nickname.trim());
  }

  if (isLoading || !user) return null;

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="프로필" backHref="/plans" />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <div className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white p-3.5">
          <Avatar name={user.name} color={user.avatarColor} size="lg" />
          <div>
            <div className="text-[15px] font-bold">{user.name}</div>
            <div className="text-[12px] text-gray-500">{user.email}</div>
          </div>
        </div>

        <Field label="닉네임">
          <FieldInput
            value={nickname}
            maxLength={20}
            onChange={(e) => setNickname(e.target.value)}
            onBlur={handleNicknameBlur}
          />
        </Field>

        <h3 className="mt-1 text-[13px] text-gray-500">내 계획</h3>
        <div>
          {plans.map((p) => {
            const role = p.members.find((m) => m.userId === user.id)?.role ?? "VIEWER";
            return (
              <Link
                key={p.id}
                href={`/plans/${p.id}`}
                className="flex items-center gap-2.5 border-b border-gray-200 py-2.5 text-[14.5px] font-semibold"
              >
                <span>{p.emoji ?? "🧳"}</span>
                <span>{p.title}</span>
                <div className="flex-1" />
                <span
                  className={
                    role === "OWNER"
                      ? "rounded-full bg-primary-soft px-2.5 py-1.5 text-[11px] font-bold text-primary-dark"
                      : "rounded-full border border-gray-200 px-2.5 py-1.5 text-[11px] font-bold text-gray-500"
                  }
                >
                  {ROLE_LABEL[role]}
                </span>
              </Link>
            );
          })}
        </div>

        <Link
          href="/profile/activities"
          className="flex items-center border-t border-gray-200 pt-4 text-[14.5px] font-semibold text-ink"
        >
          내 활동 내역
          <span className="ml-auto text-gray-500">›</span>
        </Link>

        <div className="h-1" />
        <Button onClick={handleLogout} variant="ghost" size="sm">
          로그아웃
        </Button>
      </div>
      <BottomTabBar planId={lastPlanId} />
    </div>
  );
}
