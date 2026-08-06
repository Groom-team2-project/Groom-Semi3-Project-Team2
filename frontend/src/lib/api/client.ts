/**
 * 실제 백엔드 연동 시 사용할 공통 설정.
 *
 * 지금은 모든 `lib/api/*.ts` 함수가 mock 데이터를 리턴하지만, 백엔드가 준비되면
 * 아래 `API_BASE_URL` + `apiFetch()`를 이용해 각 함수 내부만 fetch 호출로
 * 교체하면 됩니다. (함수 시그니처는 그대로 유지하는 것을 권장)
 *
 * .env.local:
 *   NEXT_PUBLIC_API_BASE_URL=https://api.tripmate.example.com
 */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

/** 지금은 API_BASE_URL이 없으니 항상 mock을 쓴다. 나중에 백엔드가 붙으면 false로. */
export const USE_MOCK = true;

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });
  if (!res.ok) {
    throw new Error(`API Error ${res.status}: ${path}`);
  }
  return res.json();
}
