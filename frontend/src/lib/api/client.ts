import { savePostLoginRedirect, shouldRedirectToLoginOn401 } from "../postLoginRedirect.ts";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

// 빈 base URL은 Netlify rewrite처럼 같은 출처의 /api를 호출한다는 뜻이다.
// 목 데이터 사용은 개발 환경에서만 명시적으로 켠다.
export const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === "true";

export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string | null;

  constructor(message: string, status: number, errorCode: string | null = null) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
    this.name = "ApiError";
  }
}

interface ApiErrorResponse {
  errorCode?: unknown;
  message?: unknown;
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

function fallbackErrorMessage(status: number): string {
  if (status === 401) return "로그인이 필요합니다.";
  if (status >= 500) return "서버에 문제가 발생했습니다.";
  return `요청에 실패했습니다. (${status})`;
}

async function toApiError(response: Response): Promise<ApiError> {
  const fallbackMessage = fallbackErrorMessage(response.status);

  try {
    const body = await response.json() as ApiErrorResponse;
    const message = typeof body.message === "string" && body.message.trim()
      ? body.message
      : fallbackMessage;
    const errorCode = typeof body.errorCode === "string" && body.errorCode.trim()
      ? body.errorCode
      : null;

    return new ApiError(message, response.status, errorCode);
  } catch {
    return new ApiError(fallbackMessage, response.status);
  }
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
    const apiError = await toApiError(response);

    if (response.status === 401) {
      if (options.retryOnUnauthorized !== false && recoverAuthentication) {
        try {
          await recoverAuthentication();
          return apiFetch<T>(path, init, { retryOnUnauthorized: false });
        } catch {
          // 재발급 실패 시 원래 요청의 인증 오류를 반환합니다.
        }
      }
      if (typeof window !== "undefined") {
        const locationPath = window.location.pathname;
        if (shouldRedirectToLoginOn401(locationPath)) {
          savePostLoginRedirect(`${locationPath}${window.location.search}`);
          window.location.replace("/login");
        }
      }
    }

    throw apiError;
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
