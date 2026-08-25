"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

const DEFAULT_POST_LOGIN_REDIRECT = "/plans";
const POST_LOGIN_REDIRECT_KEY = "postLoginRedirect";

function consumePostLoginRedirect(): string {
  const redirect = sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY);
  sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);

  // Next Router에 외부 URL이나 실행 가능한 스킴이 전달되지 않도록
  // 현재 앱의 절대 경로만 로그인 후 이동 경로로 허용합니다.
  if (redirect?.startsWith("/") && !redirect.startsWith("//")) {
    return redirect;
  }

  return DEFAULT_POST_LOGIN_REDIRECT;
}

export default function KakaoCallbackPage() {
  const router = useRouter();
  const { completeKakaoLogin } = useAuth();
  const loginTask = useRef<Promise<void> | null>(null);
  const [message, setMessage] = useState("카카오 로그인 처리 중입니다.");

  useEffect(() => {
    let active = true;

    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const state = params.get("state");

    if (!code || !state) {
      queueMicrotask(() => {
        if (active) setMessage("카카오 로그인 정보가 올바르지 않습니다.");
      });
      return () => {
        active = false;
      };
    }

    // Strict Mode에서 effect가 다시 실행돼도 로그인 요청은 공유하고,
    // 현재 effect가 정리된 뒤에는 화면 이동이나 상태 변경을 하지 않습니다.
    loginTask.current ??= completeKakaoLogin(code, state);
    void loginTask.current
      .then(() => {
        if (active) router.replace(consumePostLoginRedirect());
      })
      .catch(() => {
        if (active) setMessage("카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
      });

    return () => {
      active = false;
    };
  }, [completeKakaoLogin, router]);

  return (
    <main className="flex min-h-dvh items-center justify-center px-6 text-center">
      <p className="text-sm text-gray-600">{message}</p>
    </main>
  );
}
