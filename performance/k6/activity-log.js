import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = required("BASE_URL").replace(/\/$/, "");
const planId = required("PLAN_ID");
const profile = __ENV.PROFILE || "steady";
const accessTokens = (__ENV.ACCESS_TOKENS || required("ACCESS_TOKEN"))
  .split(",")
  .map((token) => token.trim())
  .filter(Boolean);
const pageSize = Number(__ENV.PAGE_SIZE || 20);
const sleepSeconds = Number(__ENV.SLEEP_SECONDS || 1);

const deepCursorCreatedAt = required("DEEP_CURSOR_CREATED_AT");
const deepCursorLogId = required("DEEP_CURSOR_LOG_ID");

const scenarios = {
  // 기존 측정과 같은 조건. 반복 읽기 경로의 지속 처리 여유를 확인한다.
  steady: {
    executor: "constant-vus",
    vus: Number(__ENV.VUS || 20),
    duration: __ENV.DURATION || "1m",
  },
  // 한 계획을 함께 보는 소규모 멤버의 화면 진입·추가 탐색 흐름을 확인한다.
  journey: {
    executor: "per-vu-iterations",
    vus: Number(__ENV.VUS || 6),
    iterations: Number(__ENV.ITERATIONS || 1),
    maxDuration: __ENV.MAX_DURATION || "30s",
  },
  // 실사용 인원보다 높은 읽기 요청이 짧은 시간에 모일 때의 응답 변화를 확인한다.
  ramp: {
    executor: "ramping-vus",
    startVUs: 0,
    stages: [
      { duration: "15s", target: 3 },
      { duration: "30s", target: 6 },
      { duration: "30s", target: 12 },
      { duration: "15s", target: 0 },
    ],
  },
};

if (!scenarios[profile]) {
  throw new Error("PROFILE은 steady, journey, ramp 중 하나여야 합니다.");
}

export const options = {
  scenarios: { [`activity_feed_${profile}`]: scenarios[profile] },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    checks: ["rate==1"],
    "http_req_duration{name:activity_latest}": ["p(95)<500"],
    "http_req_duration{name:activity_deep_cursor}": ["p(95)<500"],
  },
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

function required(name) {
  const value = __ENV[name];
  if (!value) {
    throw new Error(`${name} 환경 변수가 필요합니다.`);
  }
  return value;
}

function requestParams(name) {
  const accessToken = accessTokens[(__VU - 1) % accessTokens.length];
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

  // 실사용 흐름에서는 일부 사용자만 이전 활동을 추가로 탐색한다.
  const shouldReadDeepCursor = profile !== "journey" || __VU % 3 === 0;
  if (shouldReadDeepCursor) {
    const deepUrl = `${baseUrl}/api/v1/plans/${planId}/activities?size=${pageSize}`
      + `&cursorCreatedAt=${encodeURIComponent(deepCursorCreatedAt)}`
      + `&cursorLogId=${encodeURIComponent(deepCursorLogId)}`;
    const deepResponse = http.get(deepUrl, requestParams("activity_deep_cursor"));
    assertActivityPage(deepResponse, "깊은 커서 조회");
  }

  if (profile !== "journey") sleep(sleepSeconds);
}
