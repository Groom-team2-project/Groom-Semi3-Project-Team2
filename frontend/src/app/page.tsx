"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function RootPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;
    router.replace(user ? "/plans" : "/login");
  }, [isLoading, user, router]);

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-2">
      <div className="text-4xl">🧭</div>
      <div className="text-[15px] font-bold text-gray-500">트립메이트 불러오는 중…</div>
    </div>
  );
}
