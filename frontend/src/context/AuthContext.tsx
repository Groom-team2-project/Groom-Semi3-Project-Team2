"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  completeKakaoLogin as completeKakaoLoginRequest,
  loginWithKakao,
  logout as apiLogout,
} from "@/lib/api";
import type { User } from "@/lib/api";

const STORAGE_KEY = "tripmate_auth";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  loginWithKakao: () => Promise<void>;
  completeKakaoLogin: (code: string, state: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 최초 마운트 시에만 localStorage 확인 (SSR/CSR mismatch 방지를 위해 렌더 이후 useEffect에서 처리)
  useEffect(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- localStorage는 서버에 없어 마운트 후에만 읽을 수 있음
      if (saved) setUser(JSON.parse(saved) as User);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = useCallback(async () => {
    await loginWithKakao();
  }, []);

  const completeLogin = useCallback(async (code: string, state: string) => {
    const me = await completeKakaoLoginRequest(code, state);
    setUser(me);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(me));
  }, []);

  const logout = useCallback(async () => {
    await apiLogout();
    setUser(null);
    window.localStorage.removeItem(STORAGE_KEY);
  }, []);

  const value = useMemo(
    () => ({ user, isLoading, loginWithKakao: login, completeKakaoLogin: completeLogin, logout }),
    [user, isLoading, login, completeLogin, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
