"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function KakaoCallbackPage() {
  const router = useRouter();
  const { completeKakaoLogin } = useAuth();
  const requested = useRef(false);
  const [message, setMessage] = useState("카카오 로그인 처리 중입니다.");

  useEffect(() => {
    if (requested.current) return;
    requested.current = true;

    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const state = params.get("state");

    if (!code || !state) {
      queueMicrotask(() => setMessage("카카오 로그인 정보가 올바르지 않습니다."));
      return;
    }

    completeKakaoLogin(code, state)
      .then(() => router.replace("/plans"))
      .catch(() => setMessage("카카오 로그인에 실패했습니다. 다시 시도해 주세요."));
  }, [completeKakaoLogin, router]);

  return (
    <main className="flex min-h-dvh items-center justify-center px-6 text-center">
      <p className="text-sm text-gray-600">{message}</p>
    </main>
  );
}
