import { store, simulateLatency } from "./store";
import type { User } from "./types";

/**
 * 카카오 로그인 (지금은 mock).
 * 나중에는 카카오 SDK 인가 코드 → `POST /auth/kakao` 로 교체하면 됩니다.
 */
export async function loginWithKakao(): Promise<User> {
  await simulateLatency(400);
  return store.me;
}

export async function getMe(): Promise<User> {
  await simulateLatency(120);
  return store.me;
}

export async function updateMe(input: Partial<Pick<User, "name">>): Promise<User> {
  await simulateLatency();
  store.me = { ...store.me, ...input };
  return store.me;
}

export async function logout(): Promise<void> {
  await simulateLatency(150);
}
