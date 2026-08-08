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

function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAccessToken();
  const fullUrl = API_BASE_URL + path;

  const res = await fetch(fullUrl, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...init?.headers,
    },
  });

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

  return res.json();
}