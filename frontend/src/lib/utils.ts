const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

/** "2026-08-14" -> "8.14(금)" */
export function formatDateShort(iso: string): string {
  const d = new Date(iso + "T00:00:00");
  return `${d.getMonth() + 1}.${d.getDate()}(${WEEKDAYS[d.getDay()]})`;
}

/** "2026-08-14" ~ "2026-08-17" -> "8.14(금) - 8.17(월)" */
export function formatDateRange(startIso: string, endIso: string): string {
  return `${formatDateShort(startIso)} - ${formatDateShort(endIso)}`;
}

/** 오늘 기준 D-day 문자열. 과거면 null */
export function formatDday(targetIso: string, todayIso = todayISO()): string | null {
  const today = new Date(todayIso + "T00:00:00");
  const target = new Date(targetIso + "T00:00:00");
  const diff = Math.round((target.getTime() - today.getTime()) / 86400000);
  if (diff < 0) return null;
  if (diff === 0) return "D-Day";
  return `D-${diff}`;
}

export function todayISO(): string {
  return new Date().toISOString().slice(0, 10);
}

/** 여행 시작일 기준 Day 번호 -> 실제 날짜 ISO */
export function dayIndexToDate(startIso: string, dayIndex: number): string {
  const d = new Date(startIso + "T00:00:00");
  d.setDate(d.getDate() + (dayIndex - 1));
  return d.toISOString().slice(0, 10);
}

export function dateRangeToDayCount(startIso: string, endIso: string): number {
  const start = new Date(startIso + "T00:00:00");
  const end = new Date(endIso + "T00:00:00");
  return Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
}

/** ISO datetime -> "3시간 후 마감" 같은 상대 마감 문자열 */
export function formatDeadline(deadlineIso: string, nowMs = Date.now()): string {
  const diffMs = new Date(deadlineIso).getTime() - nowMs;
  if (diffMs <= 0) return "마감";
  const hours = Math.floor(diffMs / 3_600_000);
  if (hours >= 24) return `${Math.floor(hours / 24)}일 후 마감`;
  if (hours >= 1) return `${hours}시간 후 마감`;
  const minutes = Math.max(1, Math.floor(diffMs / 60_000));
  return `${minutes}분 후 마감`;
}

/** ISO datetime -> "3분 전" / "어제" / "2일 전" */
export function formatRelativeTime(iso: string, nowMs = Date.now()): string {
  const diffMs = nowMs - new Date(iso).getTime();
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "어제";
  return `${days}일 전`;
}

export function generateId(prefix = "id"): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
}

export const AVATAR_COLORS = ["#3182F6", "#00C896", "#FF9F1C", "#8B7FF2", "#F04452", "#1B64DA"];

export function cx(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}

/**
 * 계획이 완료됐는지 판단합니다.
 * endDate가 "2026-08-08"이면, 8/8 23:59:59까지는 진행 중으로 보고, 8/9 00:00:00부터 완료로 처리합니다.
 */
export function isPlanCompleted(endIso: string, nowMs = Date.now()): boolean {
  const end = new Date(endIso + "T00:00:00");
  end.setDate(end.getDate() + 1);
  return nowMs >= end.getTime();
}