"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/context/AuthContext";

export default function LoginPage() {
  const { loginWithKakao } = useAuth();
  const [pending, setPending] = useState(false);

  async function handleLogin() {
    setPending(true);
    try {
      await loginWithKakao();
    } catch {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col justify-center gap-4 px-6 pb-10">
      <div className="flex flex-col items-center gap-2 text-center">
        <div className="text-4xl">🧭</div>
        <div className="text-2xl font-extrabold">모이Go</div>
        <p className="text-[13.5px] leading-relaxed text-gray-500">
          같이 짜는 여행 일정,
          <br />
          의견 갈리면 투표로 끝.
        </p>
      </div>
      <div className="h-4" />
      <Button variant="kakao" onClick={handleLogin} disabled={pending}>
        {pending ? "로그인 중..." : "카카오로 3초 만에 시작하기"}
      </Button>
      <p className="text-center text-[11.5px] text-gray-500">카카오 로그인만 지원돼요</p>
    </div>
  );
}
