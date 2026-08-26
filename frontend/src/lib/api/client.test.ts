import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import {
  ApiError,
  apiFetch,
  clearAccessToken,
  setAuthenticationRecovery,
} from "./client.ts";

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  clearAccessToken();
  setAuthenticationRecovery(null);
});

test("백엔드 오류 응답의 errorCode와 message를 보존한다", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false,
    data: null,
    errorCode: "INVITATION_EXPIRED",
    message: "만료된 초대 링크입니다.",
  }), {
    status: 409,
    headers: { "Content-Type": "application/json" },
  });

  await assert.rejects(
    apiFetch("/api/v1/invitations/EXPIRED", undefined, {
      retryOnUnauthorized: false,
    }),
    (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.equal(error.errorCode, "INVITATION_EXPIRED");
      assert.equal(error.message, "만료된 초대 링크입니다.");
      return true;
    },
  );
});

test("재발급 실패 후에도 원래 401 응답 정보를 반환한다", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false,
    data: null,
    errorCode: "UNAUTHORIZED",
    message: "인증이 필요합니다.",
  }), {
    status: 401,
    headers: { "Content-Type": "application/json" },
  });
  setAuthenticationRecovery(async () => {
    throw new Error("Refresh Token expired");
  });

  await assert.rejects(
    apiFetch("/api/v1/plans"),
    (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 401);
      assert.equal(error.errorCode, "UNAUTHORIZED");
      assert.equal(error.message, "인증이 필요합니다.");
      return true;
    },
  );
});

test("오류 본문이 JSON이 아니면 상태별 기본 메시지를 사용한다", async () => {
  globalThis.fetch = async () => new Response("Bad Gateway", { status: 502 });

  await assert.rejects(
    apiFetch("/api/v1/plans", undefined, { retryOnUnauthorized: false }),
    (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 502);
      assert.equal(error.errorCode, null);
      assert.equal(error.message, "서버에 문제가 발생했습니다.");
      return true;
    },
  );
});
