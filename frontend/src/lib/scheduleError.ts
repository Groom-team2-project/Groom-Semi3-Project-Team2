import { ApiError } from "@/lib/api/client";

export function getScheduleLoadErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "일정을 불러오지 못했어요. 다시 시도해 주세요.";
  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return "이 일정을 조회할 권한이 없어요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";
  return "일정을 불러오지 못했어요. 다시 시도해 주세요.";
}

export function getScheduleMutationErrorMessage(
  error: unknown,
  action: "등록" | "수정" | "삭제",
): string {
  if (!(error instanceof ApiError)) return `일정 ${action}에 실패했어요. 다시 시도해 주세요.`;
  if (error.status === 0) return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
  if (error.status === 400) return error.message;
  if (error.status === 401) return "로그인이 필요해요. 다시 로그인해 주세요.";
  if (error.status === 403) return `일정을 ${action}할 권한이 없어요.`;
  if (error.status === 404) return "계획, 일정 또는 장소가 삭제되었거나 존재하지 않아요.";
  if (error.status >= 500) return "서버에 문제가 있어요. 잠시 후 다시 시도해 주세요.";
  return `일정 ${action}에 실패했어요. 다시 시도해 주세요.`;
}
