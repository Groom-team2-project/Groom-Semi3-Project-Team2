"use client";

import { useCallback, useEffect, useState } from "react";
import { getPlan } from "@/lib/api";
import type { Plan } from "@/lib/api";

export function usePlan(planId: string) {
  const [plan, setPlan] = useState<Plan | null | undefined>(undefined); // undefined: 로딩중, null: 없음
  const [version, setVersion] = useState(0);
  const [syncedKey, setSyncedKey] = useState(`${planId}:${version}`);

  const refresh = useCallback(() => setVersion((v) => v + 1), []);

  // planId/version이 바뀌면 새 데이터를 받기 전까지 다시 로딩 상태로.
  // (effect가 아니라 렌더 중 상태 조정 패턴: https://react.dev/learn/you-might-not-need-an-effect)
  const currentKey = `${planId}:${version}`;
  if (currentKey !== syncedKey) {
    setSyncedKey(currentKey);
    setPlan(undefined);
  }

  useEffect(() => {
    let alive = true;
    getPlan(planId).then((p) => {
      if (alive) setPlan(p);
    });
    return () => {
      alive = false;
    };
  }, [planId, version]);

  return { plan, isLoading: plan === undefined, refresh };
}
