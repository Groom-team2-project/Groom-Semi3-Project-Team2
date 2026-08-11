import { apiFetch, clearAccessToken, setAccessToken } from "./client";
import { store, simulateLatency } from "./store";
import type { User } from "./types";

interface CommonResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string;
}

interface KakaoAuthorizeUrlResponse {
  url: string;
  state: string;
}

interface LoginResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
  refreshTokenExpiresIn: number;
  userId: number;
  newUser: boolean;
}

interface TokenReissueResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
  refreshTokenExpiresIn: number;
}

interface UserMeResponse {
  userId: number;
  nickname: string;
  email: string | null;
  profileImage: string | null;
}

let restoreAccessTokenPromise: Promise<void> | null = null;

export function clearAuthentication(): void {
  clearAccessToken();
}

function toUser(response: UserMeResponse): User {
  const name = response.nickname.trim() || "사용자";

  return {
    id: String(response.userId),
    name,
    email: response.email ?? "",
    avatarColor: "#FEE500",
    avatarInitial: name.slice(0, 1),
  };
}

export async function loginWithKakao(): Promise<void> {
  const response = await apiFetch<CommonResponse<KakaoAuthorizeUrlResponse>>(
    "/api/v1/auth/kakao/authorize-url",
  );

  window.location.assign(response.data.url);
}

export async function completeKakaoLogin(code: string, state: string): Promise<User> {
  const response = await apiFetch<CommonResponse<LoginResponse>>(
    "/api/v1/auth/kakao/login",
    {
      method: "POST",
      body: JSON.stringify({ code, state }),
    },
  );

  setAccessToken(response.data.accessToken);

  try {
    return await getMe();
  } catch (error) {
    clearAuthentication();
    throw error;
  }
}

export function restoreAccessToken(): Promise<void> {
  if (!restoreAccessTokenPromise) {
    restoreAccessTokenPromise = apiFetch<CommonResponse<TokenReissueResponse>>(
      "/api/v1/auth/reissue",
      { method: "POST" },
    )
      .then((response) => {
        setAccessToken(response.data.accessToken);
      })
      .catch((error) => {
        clearAuthentication();
        throw error;
      })
      .finally(() => {
        restoreAccessTokenPromise = null;
      });
  }

  return restoreAccessTokenPromise;
}

export async function getMe(): Promise<User> {
  const response = await apiFetch<CommonResponse<UserMeResponse>>("/api/v1/users/me");
  return toUser(response.data);
}

// 프로필 수정 API가 구현될 때 실제 백엔드 호출로 교체합니다.
export async function updateMe(input: Partial<Pick<User, "name">>): Promise<User> {
  await simulateLatency();
  store.me = { ...store.me, ...input };
  return store.me;
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<CommonResponse<null>>("/api/v1/auth/logout", {
      method: "POST",
    });
  } finally {
    clearAuthentication();
  }
}
