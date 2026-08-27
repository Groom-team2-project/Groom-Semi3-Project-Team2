import assert from "node:assert/strict";
import test from "node:test";
import {
  consumePostLoginRedirect,
  savePostLoginRedirect,
} from "./postLoginRedirect.ts";

const CURRENT_ORIGIN = "https://moigo.netlify.app";

function storageWith(
  redirect: string | null,
): Pick<Storage, "getItem" | "removeItem" | "setItem"> {
  return {
    getItem: () => redirect,
    removeItem: () => undefined,
    setItem: () => undefined,
  };
}

test("로그인 후 이동 경로를 sessionStorage에 저장한다", () => {
  let savedKey = "";
  let savedRedirect = "";
  const storage = storageWith(null);
  storage.setItem = (key, value) => {
    savedKey = key;
    savedRedirect = value;
  };

  const saved = savePostLoginRedirect(
    "/invitations/LOCAL123",
    () => storage,
  );

  assert.equal(saved, true);
  assert.equal(savedKey, "postLoginRedirect");
  assert.equal(savedRedirect, "/invitations/LOCAL123");
});

test("sessionStorage.setItem이 거부되면 실패를 반환한다", () => {
  const storage = storageWith(null);
  storage.setItem = () => {
    throw new DOMException("Access denied", "SecurityError");
  };

  const saved = savePostLoginRedirect("/invitations/LOCAL123", () => storage);

  assert.equal(saved, false);
});

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
