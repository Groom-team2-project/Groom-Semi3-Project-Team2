"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  completeKakaoLogin as completeKakaoLoginRequest,
  loginWithKakao,
  logout as apiLogout,
  restoreAccessToken,
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
    let active = true;

    async function initializeAuth() {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (!saved) return;

      try {
        const savedUser = JSON.parse(saved) as User;
        await restoreAccessToken();
        if (active) setUser(savedUser);
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    }

    void initializeAuth().finally(() => {
      if (active) setIsLoading(false);
    });

    return () => {
      active = false;
    };
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
    try {
      await apiLogout();
    } finally {
      setUser(null);
      window.localStorage.removeItem(STORAGE_KEY);
    }
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
