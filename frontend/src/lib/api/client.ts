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

export const USE_MOCK = !API_BASE_URL;

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

const ACCESS_TOKEN_KEY = "tripmate_access_token";
const REFRESH_TOKEN_KEY = "tripmate_refresh_token";

function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function saveAuthTokens(accessToken: string, refreshToken: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearAuthTokens(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAccessToken();
  const fullUrl = API_BASE_URL + path;

  // Headers 인스턴스로 정규화 — init.headers가 Headers/배열/객체 어떤 형태로 와도 올바르게 병합되고,
  // 이미 값이 있으면 기본값(Content-Type, Authorization)으로 덮어쓰지 않는다.
  const headers = new Headers(init?.headers);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", "Bearer " + token);
  }

  let res: Response;
  try {
    res = await fetch(fullUrl, {
      ...init,
      headers,
      credentials: "include",
    });
  } catch {
    // 네트워크 장애, 요청 취소(AbortController) 등 fetch 자체가 실패한 경우
    throw new ApiError("요청을 보내지 못했어요 (네트워크 오류 또는 요청 취소)", 0);
  }

  if (!res.ok) {
    if (res.status === 401) {
      if (typeof window !== "undefined") {
        // eslint-disable-next-line @next/next/no-location-assign-relative-destination
        window.location.href = "/login";
      }
      throw new ApiError("로그인이 필요해요", 401);
    }

    if (res.status >= 500) {
      throw new ApiError("서버에 문제가 생겼어요. 잠시 후 다시 시도해주세요.", res.status);
    }

    throw new ApiError("요청에 실패했어요 (" + res.status + ")", res.status);
  }

  // 204 No Content — 응답 본문이 없으므로 res.json()을 시도하지 않는다.
  if (res.status === 204) {
    return undefined as T;
  }

  try {
    return await res.json();
  } catch {
    throw new ApiError("서버 응답을 처리하지 못했어요", res.status);
  }
}
