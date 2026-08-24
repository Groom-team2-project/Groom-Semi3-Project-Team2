import { ApiError } from "@/lib/api/client";

export function getActivityErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "활동 내역을 불러오지 못했어요. 다시 시도해 주세요.";
  }

  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return "활동 내역을 볼 수 있는 권한이 없어요.";
  if (error.status === 404) return "더 이상 볼 수 있는 활동이 없어요.";
  if (error.status === 400) return "요청이 올바르지 않아요. 새로고침 후 다시 시도해 주세요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";

  return "활동 내역을 불러오지 못했어요. 다시 시도해 주세요.";
}
