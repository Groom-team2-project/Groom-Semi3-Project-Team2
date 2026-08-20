import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = required("BASE_URL").replace(/\/$/, "");
const planId = required("PLAN_ID");
const accessToken = required("ACCESS_TOKEN");
const pageSize = Number(__ENV.PAGE_SIZE || 20);
const sleepSeconds = Number(__ENV.SLEEP_SECONDS || 1);

const deepCursorCreatedAt = __ENV.DEEP_CURSOR_CREATED_AT;
const deepCursorLogId = __ENV.DEEP_CURSOR_LOG_ID;

export const options = {
  scenarios: {
    activity_feed_read: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || "1m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{name:activity_latest}": ["p(95)<500"],
    "http_req_duration{name:activity_deep_cursor}": ["p(95)<500"],
  },
};

function required(name) {
  const value = __ENV[name];
  if (!value) {
    throw new Error(`${name} 환경 변수가 필요합니다.`);
  }
  return value;
}

function requestParams(name) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: "application/json",
    },
    tags: { name },
  };
}

function assertActivityPage(response, name) {
  return check(response, {
    [`${name}: 200 응답`]: (res) => res.status === 200,
    [`${name}: 공통 응답 성공`]: (res) => res.status === 200 && res.json("success") === true,
    [`${name}: 활동 목록 존재`]: (res) => Array.isArray(res.json("data.activities")),
  });
}

export default function () {
  const latestResponse = http.get(
    `${baseUrl}/api/v1/plans/${planId}/activities?size=${pageSize}`,
    requestParams("activity_latest"),
  );
  assertActivityPage(latestResponse, "최신 활동 조회");

  // 중간 이후 커서를 전달하면 OFFSET 없이 다음 페이지를 탐색하는 성능도 함께 측정한다.
  if (deepCursorCreatedAt && deepCursorLogId) {
    const deepUrl = `${baseUrl}/api/v1/plans/${planId}/activities?size=${pageSize}`
      + `&cursorCreatedAt=${encodeURIComponent(deepCursorCreatedAt)}`
      + `&cursorLogId=${encodeURIComponent(deepCursorLogId)}`;
    const deepResponse = http.get(deepUrl, requestParams("activity_deep_cursor"));
    assertActivityPage(deepResponse, "깊은 커서 조회");
  }

  sleep(sleepSeconds);
}
