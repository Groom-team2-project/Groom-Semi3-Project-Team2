"use client";

import { useEffect, useRef, useState } from "react";

/**
 * 장소 검색처럼 다른 페이지로 갔다가 돌아와야 하는 폼의 임시 상태를
 * sessionStorage에 보관해 입력값이 날아가지 않게 해줍니다.
 */
export function useFormDraft<T>(key: string, initial: T) {
  const [draft, setDraft] = useState<T>(() => {
    if (typeof window === "undefined") return initial;
    try {
      const raw = window.sessionStorage.getItem(key);
      return raw ? { ...initial, ...JSON.parse(raw) } : initial;
    } catch {
      return initial;
    }
  });

  const isFirstRender = useRef(true);
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    try {
      window.sessionStorage.setItem(key, JSON.stringify(draft));
    } catch {
      // ignore
    }
  }, [key, draft]);

  function clearDraft() {
    try {
      window.sessionStorage.removeItem(key);
    } catch {
      // ignore
    }
  }

  return { draft, setDraft, clearDraft };
}

/** 세션에 이미 저장된 draft가 있는지 (장소 검색에서 돌아온 경우인지) 확인 */
export function hasDraft(key: string): boolean {
  if (typeof window === "undefined") return false;
  try {
    return window.sessionStorage.getItem(key) !== null;
  } catch {
    return false;
  }
}
