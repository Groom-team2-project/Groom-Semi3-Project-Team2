export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

// 빈 base URL은 Netlify rewrite처럼 같은 출처의 /api를 호출한다는 뜻이다.
// 목 데이터 사용은 개발 환경에서만 명시적으로 켠다.
export const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

let accessToken: string | null = null;
let recoverAuthentication: (() => Promise<void>) | null = null;

interface ApiFetchOptions {
  retryOnUnauthorized?: boolean;
}

export function setAccessToken(token: string): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

export function setAuthenticationRecovery(
  recovery: (() => Promise<void>) | null,
): void {
  recoverAuthentication = recovery;
}

export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
  options: ApiFetchOptions = {},
): Promise<T> {
  const fullUrl = API_BASE_URL + path;
  const headers = new Headers(init?.headers);

  if (!headers.has("Content-Type") && !(init?.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(fullUrl, {
      ...init,
      headers,
      credentials: "include",
    });
  } catch {
    throw new ApiError("요청을 보내지 못했습니다.", 0);
  }

  if (!response.ok) {
    if (response.status === 401) {
      if (options.retryOnUnauthorized !== false && recoverAuthentication) {
        try {
          await recoverAuthentication();
          return apiFetch<T>(path, init, { retryOnUnauthorized: false });
        } catch {
          // 재발급 실패 시 원래 요청의 인증 오류를 반환합니다.
        }
      }
      throw new ApiError("로그인이 필요합니다.", 401);
    }
    if (response.status >= 500) {
      throw new ApiError("서버에 문제가 발생했습니다.", response.status);
    }
    throw new ApiError(`요청에 실패했습니다. (${response.status})`, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  try {
    return await response.json();
  } catch {
    throw new ApiError("서버 응답을 처리하지 못했습니다.", response.status);
  }
}
