import {
  apiFetch,
  clearAuthTokens,
  getRefreshToken,
  saveAuthTokens,
} from "./client";
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
  refreshToken: string;
  refreshTokenExpiresIn: number;
  userId: number;
  newUser: boolean;
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

  saveAuthTokens(response.data.accessToken, response.data.refreshToken);

  // 사용자 상세 조회 API가 생기기 전까지 로그인 응답으로 구성하는 최소 사용자 정보입니다.
  return {
    id: String(response.data.userId),
    name: "카카오 사용자",
    email: "",
    avatarColor: "#FEE500",
    avatarInitial: "카",
  };
}

// 프로필 API가 연결될 때 실제 백엔드 호출로 교체합니다.
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
  const refreshToken = getRefreshToken();

  try {
    if (refreshToken) {
      await apiFetch<CommonResponse<null>>("/api/v1/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      });
    }
  } finally {
    clearAuthTokens();
  }
}
