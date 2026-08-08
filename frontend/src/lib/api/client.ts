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

let accessToken: string | null = null;

export function setAccessToken(token: string): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const fullUrl = API_BASE_URL + path;
  const headers = new Headers(init?.headers);

  if (!headers.has("Content-Type")) {
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
