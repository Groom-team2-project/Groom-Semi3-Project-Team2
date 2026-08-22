"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import {
  completeKakaoLogin as completeKakaoLoginRequest,
  loginWithKakao,
  logout as apiLogout,
  restoreAuthentication,
  subscribeAuthenticationCleared,
  updateProfile as updateProfileRequest,
} from "@/lib/api";
import type { User } from "@/lib/api";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  loginWithKakao: () => Promise<void>;
  completeKakaoLogin: (code: string, state: string) => Promise<void>;
  updateProfile: (nickname: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  const [shouldRestoreOnMount] = useState(
    () => pathname !== "/oauth/kakao/callback",
  );
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(shouldRestoreOnMount);

  useEffect(() => {
    let active = true;

    const unsubscribe = subscribeAuthenticationCleared(() => {
      if (active) setUser(null);
    });

    if (!shouldRestoreOnMount) {
      return () => {
        active = false;
        unsubscribe();
      };
    }

    async function initializeAuth() {
      try {
        const me = await restoreAuthentication();
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
      unsubscribe();
    };
  }, [shouldRestoreOnMount]);

  const login = useCallback(async () => {
    await loginWithKakao();
  }, []);

  const completeLogin = useCallback(async (code: string, state: string) => {
    const me = await completeKakaoLoginRequest(code, state);
    setUser(me);
  }, []);

  const updateProfile = useCallback(async (nickname: string) => {
    const profile = await updateProfileRequest(nickname);
    setUser(profile);
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isLoading,
      loginWithKakao: login,
      completeKakaoLogin: completeLogin,
      updateProfile,
      logout,
    }),
    [user, isLoading, login, completeLogin, updateProfile, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
