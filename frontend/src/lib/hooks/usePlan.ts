"use client";

import { useCallback, useEffect, useState } from "react";
import { getPlan } from "@/lib/api";
import type { Plan } from "@/lib/api";

export function usePlan(planId: string) {
  const [plan, setPlan] = useState<Plan | null | undefined>(undefined); // undefined: 로딩중, null: 없음
  const [error, setError] = useState<unknown>(null);
  const [version, setVersion] = useState(0);
  const [syncedKey, setSyncedKey] = useState(`${planId}:${version}`);

  const refresh = useCallback(() => setVersion((v) => v + 1), []);

  // planId/version 변경 시 새 데이터가 오기 전까지 로딩 상태로 리셋
  const currentKey = `${planId}:${version}`;
  if (currentKey !== syncedKey) {
    setSyncedKey(currentKey);
    setPlan(undefined);
    setError(null);
  }

  useEffect(() => {
    let alive = true;
    getPlan(planId)
      .then((p) => {
        if (alive) setPlan(p);
      })
      .catch((err) => {
        // getPlan은 403/404만 null로 바꿔주므로, 여기 도달하는 건 재시도 가능한 진짜 오류임
        if (alive) setError(err);
      });
    return () => {
      alive = false;
    };
  }, [planId, version]);

  return { plan, isLoading: plan === undefined && !error, error, refresh };
}
