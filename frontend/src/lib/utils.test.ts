import assert from "node:assert/strict";
import test from "node:test";
import {
  dateRangeToDayCount,
  dayIndexToDate,
  formatDateShort,
  formatDday,
  todayISO,
} from "./utils.ts";

process.env.TZ = "Asia/Seoul";

test("여행 시작일을 Day 1 날짜로 그대로 반환한다", () => {
  assert.equal(dayIndexToDate("2026-08-21", 1), "2026-08-21");
});

test("월과 연도를 넘어가는 Day 날짜를 계산한다", () => {
  assert.equal(dayIndexToDate("2026-12-31", 2), "2027-01-01");
});

test("달력 날짜를 실행 환경의 타임존과 무관하게 표시한다", () => {
  assert.equal(formatDateShort("2026-08-21"), "8.21(금)");
});

test("여행 기간과 D-day를 달력 날짜 기준으로 계산한다", () => {
  assert.equal(dateRangeToDayCount("2026-08-21", "2026-08-23"), 3);
  assert.equal(formatDday("2026-08-21", "2026-08-20"), "D-1");
});

test("오늘 날짜는 UTC가 아닌 사용자 현지 날짜를 반환한다", () => {
  const shortlyAfterMidnightInSeoul = new Date("2026-08-20T15:30:00Z");
  assert.equal(todayISO(shortlyAfterMidnightInSeoul), "2026-08-21");
});
