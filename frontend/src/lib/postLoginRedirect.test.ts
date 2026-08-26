import assert from "node:assert/strict";
import test from "node:test";
import { consumePostLoginRedirect } from "./postLoginRedirect.ts";

const CURRENT_ORIGIN = "https://moigo.netlify.app";

function storageWith(
  redirect: string | null,
): Pick<Storage, "getItem" | "removeItem"> {
  return {
    getItem: () => redirect,
    removeItem: () => undefined,
  };
}

test("같은 origin의 앱 내부 경로를 반환한다", () => {
  const redirect = consumePostLoginRedirect(
    () => storageWith("/invitations/LOCAL123?source=qr#join"),
    CURRENT_ORIGIN,
  );

  assert.equal(redirect, "/invitations/LOCAL123?source=qr#join");
});

test("이중 슬래시로 시작하는 외부 URL을 차단한다", () => {
  const redirect = consumePostLoginRedirect(
    () => storageWith("//evil.example"),
    CURRENT_ORIGIN,
  );

  assert.equal(redirect, "/plans");
});

test("백슬래시로 우회한 외부 URL을 차단한다", () => {
  const redirect = consumePostLoginRedirect(
    () => storageWith("/\\evil.example"),
    CURRENT_ORIGIN,
  );

  assert.equal(redirect, "/plans");
});

test("sessionStorage 접근이 거부되면 기본 경로를 반환한다", () => {
  const redirect = consumePostLoginRedirect(
    () => {
      throw new DOMException("Access denied", "SecurityError");
    },
    CURRENT_ORIGIN,
  );

  assert.equal(redirect, "/plans");
});
