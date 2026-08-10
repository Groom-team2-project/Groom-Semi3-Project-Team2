"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  completeKakaoLogin as completeKakaoLoginRequest,
  getMe,
  loginWithKakao,
  logout as apiLogout,
  restoreAccessToken,
} from "@/lib/api";
import type { User } from "@/lib/api";

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

  useEffect(() => {
    let active = true;

    async function initializeAuth() {
      try {
        await restoreAccessToken();
        const me = await getMe();
        if (active) setUser(me);
      } catch {
        if (active) setUser(null);
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
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } finally {
      setUser(null);
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
