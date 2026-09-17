import assert from "node:assert/strict";
import { test } from "node:test";
import { buildMapSearchPath } from "./mapPlaceSearch.ts";

const bounds = { southWestLongitude: 127, southWestLatitude: 37, northEastLongitude: 128, northEastLatitude: 38 };
test("키워드만 검색하면 지도 제한과 카테고리는 생략한다", () => {
  const url = new URL(buildMapSearchPath({ keyword: " 스타벅스 " })!, "http://localhost");
  assert.equal(url.pathname, "/api/v1/place/search");
  assert.equal(url.searchParams.get("keyword"), "스타벅스");
  assert.equal(url.searchParams.has("categoryGroupCode"), false);
  assert.equal(url.searchParams.has("southWestLongitude"), false);
});
test("키워드와 단일 카테고리, 지도 영역은 동시에 전달한다", () => {
  const url = new URL(buildMapSearchPath({ keyword: "스타벅스", category: "CE7", bounds }, 2)!, "http://localhost");
  assert.equal(url.searchParams.get("categoryGroupCode"), "CE7");
  assert.equal(url.searchParams.get("page"), "2");
  for (const [key, value] of Object.entries(bounds)) assert.equal(url.searchParams.get(key), String(value));
});
test("주소로 이동한 뒤 카테고리 검색은 주소를 키워드로 보내지 않는다", () => {
  const url = new URL(buildMapSearchPath({ category: "CE7", bounds })!, "http://localhost");
  assert.equal(url.pathname, "/api/v1/place/category/CE7");
  assert.equal(url.searchParams.has("keyword"), false);
});
test("카테고리 해제 후 키워드와 지도 영역은 유지한다", () => {
  const url = new URL(buildMapSearchPath({ keyword: "카페", bounds })!, "http://localhost");
  assert.equal(url.searchParams.has("categoryGroupCode"), false);
  assert.equal(url.searchParams.get("southWestLongitude"), "127");
});
test("조건이 모두 없으면 검색하지 않고 카테고리만 있으면 지도 영역을 요구한다", () => {
  assert.equal(buildMapSearchPath({}), null);
  assert.equal(buildMapSearchPath({ keyword: "  " }), null);
  assert.throws(() => buildMapSearchPath({ category: "CE7" }));
});
