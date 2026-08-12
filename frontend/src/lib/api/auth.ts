import {
  ApiError,
  apiFetch,
  clearAccessToken,
  setAccessToken,
  setAuthenticationRecovery,
} from "./client";
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

interface UserProfileResponse {
  userId: number;
  nickname: string;
  email: string | null;
  profileImage: string | null;
}

interface RestoreAccessTokenTask {
  generation: number;
  promise: Promise<void>;
}

let authenticationGeneration = 0;
let restoreAccessTokenTask: RestoreAccessTokenTask | null = null;
const authenticationClearedListeners = new Set<() => void>();

class SupersededAuthenticationError extends Error {
  constructor() {
    super("A newer authentication operation has superseded this one.");
    this.name = "SupersededAuthenticationError";
  }
}

export function clearAuthentication(): void {
  authenticationGeneration += 1;
  clearAccessToken();
  authenticationClearedListeners.forEach((listener) => listener());
}

export function subscribeAuthenticationCleared(listener: () => void): () => void {
  authenticationClearedListeners.add(listener);
  return () => authenticationClearedListeners.delete(listener);
}

function beginAuthentication(): number {
  clearAuthentication();
  return authenticationGeneration;
}

function assertCurrentAuthentication(generation: number): void {
  if (generation !== authenticationGeneration) {
    throw new SupersededAuthenticationError();
  }
}

function clearRejectedAuthentication(error: unknown, generation: number): void {
  if (
    generation === authenticationGeneration
    && error instanceof ApiError
    && error.status === 401
  ) {
    clearAuthentication();
  }
}

function toUser(response: UserProfileResponse): User {
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
  const generation = beginAuthentication();

  try {
    const response = await apiFetch<CommonResponse<LoginResponse>>(
      "/api/v1/auth/kakao/login",
      {
        method: "POST",
        body: JSON.stringify({ code, state }),
      },
      { retryOnUnauthorized: false },
    );

    assertCurrentAuthentication(generation);
    setAccessToken(response.data.accessToken);

    const profile = await getProfile();
    assertCurrentAuthentication(generation);
    return profile;
  } catch (error) {
    clearRejectedAuthentication(error, generation);
    throw error;
  }
}

export function restoreAccessToken(): Promise<void> {
  const generation = authenticationGeneration;

  if (restoreAccessTokenTask?.generation === generation) {
    return restoreAccessTokenTask.promise;
  }

  const promise = apiFetch<CommonResponse<TokenReissueResponse>>(
      "/api/v1/auth/reissue",
      { method: "POST" },
      { retryOnUnauthorized: false },
    )
      .then((response) => {
        assertCurrentAuthentication(generation);
        setAccessToken(response.data.accessToken);
      })
      .catch((error) => {
        clearRejectedAuthentication(error, generation);
        throw error;
      })
      .finally(() => {
        if (restoreAccessTokenTask?.promise === promise) {
          restoreAccessTokenTask = null;
        }
      });

  restoreAccessTokenTask = { generation, promise };
  return promise;
}

export async function restoreAuthentication(): Promise<User> {
  const generation = authenticationGeneration;

  try {
    await restoreAccessToken();
    assertCurrentAuthentication(generation);

    const profile = await getProfile();
    assertCurrentAuthentication(generation);
    return profile;
  } catch (error) {
    clearRejectedAuthentication(error, generation);
    throw error;
  }
}

export async function getProfile(): Promise<User> {
  const response = await apiFetch<CommonResponse<UserProfileResponse>>(
    "/api/v1/users/profile",
  );
  return toUser(response.data);
}

export async function updateProfile(nickname: string): Promise<User> {
  const response = await apiFetch<CommonResponse<UserProfileResponse>>(
    "/api/v1/users/profile",
    {
      method: "PATCH",
      body: JSON.stringify({ nickname }),
    },
  );

  return toUser(response.data);
}

export async function logout(): Promise<void> {
  const generation = beginAuthentication();

  try {
    await apiFetch<CommonResponse<null>>("/api/v1/auth/logout", {
      method: "POST",
    }, { retryOnUnauthorized: false });
  } finally {
    if (generation === authenticationGeneration) {
      clearAccessToken();
    }
  }
}

setAuthenticationRecovery(restoreAccessToken);
