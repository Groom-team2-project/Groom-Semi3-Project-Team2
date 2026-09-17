import assert from "node:assert/strict";
import { test } from "node:test";
import { straightLineDistance, formatRouteDistance } from "./routeDistance.ts";

test("같은 장소는 0m이며 좌표 순서를 바꿔도 거리는 같다", () => {
  const a = { latitude: 37, longitude: 127 };
  const b = { latitude: 37.01, longitude: 127.02 };
  assert.equal(straightLineDistance(a, a), 0);
  assert.equal(straightLineDistance(a, b), straightLineDistance(b, a));
});
test("적도에서 경도 1도 차이는 약 111.2km이다", () => {
  assert.ok(Math.abs(straightLineDistance({ latitude: 0, longitude: 0 }, { latitude: 0, longitude: 1 }) - 111195) < 1);
});
test("짧은 거리는 m, 1km 이상은 km로 표시한다", () => {
  assert.equal(formatRouteDistance(0), "0 m");
  assert.equal(formatRouteDistance(240.4), "240 m");
  assert.equal(formatRouteDistance(999.9), "1.0 km");
  assert.equal(formatRouteDistance(1250), "1.3 km");
});
