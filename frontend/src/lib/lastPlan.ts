"use client";

import { useEffect, useState } from "react";

const KEY = "tripmate_last_plan_id";

/** 마지막으로 들어간 계획 id를 기억해서, /profile 같은 계획 밖 화면에서도 하단 탭바가 동작하게 함 */
export function setLastPlanId(planId: string) {
  try {
    window.localStorage.setItem(KEY, planId);
  } catch {
    // ignore
  }
}

export function useLastPlanId(): string | null {
  const [planId, setPlanId] = useState<string | null>(null);
  useEffect(() => {
    // localStorage는 서버에 없어 마운트 후에만 읽을 수 있음
    try {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPlanId(window.localStorage.getItem(KEY));
    } catch {
      setPlanId(null);
    }
  }, []);
  return planId;
}

/** 계획 상세로 진입하는 화면에서 호출해 마지막 계획을 기록 */
export function useRememberPlan(planId: string) {
  useEffect(() => {
    setLastPlanId(planId);
  }, [planId]);
}
