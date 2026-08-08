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

  // 사용자 상세 조회 API가 생기기 전까지 로그인 응답으로 구성하는 최소 사용자 정보입니다.
  return {
    id: String(response.data.userId),
    name: "카카오 사용자",
    email: "",
    avatarColor: "#FEE500",
    avatarInitial: "카",
  };
}

export async function restoreAccessToken(): Promise<void> {
  const response = await apiFetch<CommonResponse<TokenReissueResponse>>(
    "/api/v1/auth/reissue",
    { method: "POST" },
  );

  setAccessToken(response.data.accessToken);
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
  try {
    await apiFetch<CommonResponse<null>>("/api/v1/auth/logout", {
      method: "POST",
    });
  } finally {
    clearAccessToken();
  }
}
