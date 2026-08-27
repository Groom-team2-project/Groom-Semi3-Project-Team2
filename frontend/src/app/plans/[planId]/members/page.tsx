"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/FieldInput";
import { MemberRow } from "@/components/plan/MemberRow";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import {
  deletePlan,
  getInvitation,
  getCurrentInvitation,
  getMembers,
  leavePlan,
  updateMemberRole,
} from "@/lib/api";
import { shareInviteToKakao } from "@/lib/kakao"; // [신규]
import type { Invitation, Member, Role } from "@/lib/api";
import { isPlanCompleted } from "@/lib/utils";

type ConfirmAction = "delete" | "leave" | null;

export default function MembersPage({
                                      params,
                                    }: {
  params: Promise<{ planId: string }>;
}) {
  const { planId } = use(params);
  const router = useRouter();
  const { plan, isLoading } = usePlan(planId);

  const [invitation, setInvitation] = useState<Invitation | null>(null);
  const [invitationFailed, setInvitationFailed] = useState(false);
  const [copied, setCopied] = useState(false);
  const [members, setMembers] = useState<Member[]>([]);

  const [confirmAction, setConfirmAction] =
      useState<ConfirmAction>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState("");

  const myRole = plan?.myRole;
  const isOwner = myRole === "OWNER";

  useEffect(() => {
    // [변경] OWNER는 생성/재사용 API(POST), OWNER가 아니면 조회 전용 API(GET)를 호출합니다.
    // isOwner는 plan 로딩이 끝나야 정확한 값이 나오므로, plan이 없으면(로딩 중) 아직 호출하지 않습니다.
    if (!plan) {
      return;
    }

    const fetchInvitation = isOwner
        ? getInvitation(planId)
        : getCurrentInvitation(planId);

    fetchInvitation
        .then((data) => {
          setInvitation(data);
          setInvitationFailed(false);
        })
        .catch(() => {
          // OWNER가 아직 링크를 한 번도 안 만든 경우, EDITOR/VIEWER는
          // "아직 초대 링크가 없습니다" 상태를 이 catch로 받게 됩니다.
          setInvitation(null);
          setInvitationFailed(true);
        });

    getMembers(planId)
        .then(setMembers)
        .catch(() => {
          setMembers([]);
        });
  }, [planId, plan, isOwner]);

  if (isLoading) {
    return null;
  }

  if (!plan) {
    return <PlanNotFound />;
  }

  const planCompleted = isPlanCompleted(plan.endDate);
  const planTitle = plan.title;

  let roleDescription =
      "이 계획에 참여 중인 멤버와 역할을 확인할 수 있습니다.";

  if (myRole === "OWNER") {
    roleDescription =
        "초대 링크로 참여하면 편집자로 추가됩니다. 모임장은 역할 태그를 눌러 멤버를 편집자 ↔ 뷰어로 변경할 수 있습니다.";
  }

  if (myRole === "EDITOR") {
    roleDescription =
        "편집자는 계획과 일정을 편집할 수 있습니다. 초대 링크를 복사하거나 카카오톡으로 공유할 수 있지만, 새 링크 발급과 멤버 역할 변경은 모임장만 할 수 있습니다.";
  }

  if (myRole === "VIEWER") {
    roleDescription =
        "뷰어는 계획과 일정을 조회할 수 있습니다. 초대 링크를 복사하거나 카카오톡으로 공유할 수 있지만, 수정과 새 링크 발급, 멤버 역할 변경은 할 수 없습니다.";
  }

  async function handleCopy() {
    if (!invitation) {
      return;
    }

    try {
      await navigator.clipboard.writeText(invitation.url);
      setCopied(true);
      setTimeout(() => {
        setCopied(false);
      }, 1500);
    } catch {
      // 클립보드 권한이 없는 환경에서는 아무 동작도 하지 않음
    }
  }

  function handleKakaoShare() {
    if (!invitation) {
      return;
    }
    shareInviteToKakao(planTitle, invitation.url);
  }

  async function handleRoleChange(
      memberId: string,
      role: Role,
  ) {
    await updateMemberRole(planId, memberId, role);
    const updatedMembers = await getMembers(planId);
    setMembers(updatedMembers);
  }

  function openConfirm(action: ConfirmAction) {
    setActionError("");
    setConfirmAction(action);
  }

  function closeConfirm() {
    if (isSubmitting) {
      return;
    }
    setActionError("");
    setConfirmAction(null);
  }

  async function handleConfirm() {
    if (!confirmAction) {
      return;
    }

    setIsSubmitting(true);
    setActionError("");

    try {
      if (confirmAction === "delete") {
        await deletePlan(planId);
      }

      if (confirmAction === "leave") {
        await leavePlan(planId);
      }

      router.refresh();
      router.replace("/plans");
    } catch (error) {
      console.error(`[${confirmAction}] plan action failed:`, error);

      const detail =
          error instanceof Error ? error.message : String(error);
      const status =
          error && typeof error === "object" && "status" in error
              ? (error as { status?: number }).status
              : undefined;

      if (confirmAction === "delete") {
        setActionError(
            `계획을 삭제하지 못했습니다.${status ? ` (${status})` : ""} ${detail}`,
        );
      } else {
        setActionError(
            `계획에서 나가지 못했습니다.${status ? ` (${status})` : ""} ${detail}`,
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
      <div className="flex min-h-dvh flex-col">
        <AppBar
            title="멤버 관리"
            backHref={`/plans/${planId}`}
        />

        <div className="flex flex-1 flex-col px-4 pb-8">
          {/* 초대 링크 —  OWNER 전용이 아니라 모든 멤버에게 노출합니다.
              발급(생성/재발급)만 OWNER 전용 */}
          <div className="flex flex-col gap-3">
            <Field label="초대 링크">
              <div className="flex items-center gap-2 rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 font-mono text-[12px] text-gray-700">
    <span className="truncate">
      {planCompleted
          ? "종료된 계획이라 더 이상 초대할 수 없어요."
          : invitation
              ? invitation.url
              : invitationFailed
                  ? isOwner
                      ? "초대 링크를 불러올 수 없습니다."
                      : "아직 발급된 초대 링크가 없어요. 모임장에게 요청해보세요."
                  : "링크 생성 중..."}
    </span>

                <span className="flex-1" />

                <button
                    type="button"
                    onClick={handleCopy}
                    disabled={!invitation || planCompleted}
                    className="shrink-0 font-bold text-primary disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {copied ? "복사됨" : "복사"}
                </button>
              </div>
            </Field>

            <Button
                variant="kakao"
                disabled={!invitation || planCompleted}
                onClick={handleKakaoShare}
            >
              카카오톡으로 초대하기
            </Button>
          </div>

          {/* 참여 멤버 */}
          <div className="mt-5">
            <h3 className="text-[13px] text-gray-500">
              참여 멤버 ({members.length}/{plan.capacity ?? "∞"}명)
            </h3>

            <div className="mt-1">
              {members.map((member) => (
                  <MemberRow
                      key={member.id}
                      member={member}
                      editable={isOwner}
                      onChangeRole={(role) =>
                          handleRoleChange(member.id, role)
                      }
                  />
              ))}
            </div>
          </div>

          {/* 역할별 안내 */}
          <p className="mt-4 text-[12px] leading-relaxed text-gray-500">
            {roleDescription}
          </p>

          {/* 계획 삭제 / 계획 나가기 */}
          <div className="mt-auto pt-10">
            <div className="border-t border-gray-200 pt-5">
              {isOwner ? (
                  <button
                      type="button"
                      onClick={() => openConfirm("delete")}
                      className="w-full py-2 text-center text-[13px] font-semibold text-red"
                  >
                    계획 삭제
                  </button>
              ) : (
                  <button
                      type="button"
                      onClick={() => openConfirm("leave")}
                      className="w-full py-2 text-center text-[13px] font-semibold text-red"
                  >
                    계획 나가기
                  </button>
              )}
            </div>
          </div>
        </div>

        {/* 삭제 / 나가기 확인 모달 */}
        {confirmAction && (
            <div
                className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 px-4 pb-4 sm:items-center sm:pb-0"
                onClick={closeConfirm}
            >
              <div
                  role="dialog"
                  aria-modal="true"
                  className="w-full max-w-sm rounded-3xl bg-white p-5 shadow-xl"
                  onClick={(event) => {
                    event.stopPropagation();
                  }}
              >
                <h2 className="text-[18px] font-bold text-ink">
                  {confirmAction === "delete"
                      ? "이 계획을 삭제할까요?"
                      : "이 계획에서 나갈까요?"}
                </h2>

                <p className="mt-2 text-[13px] leading-relaxed text-gray-500">
                  {confirmAction === "delete" ? (
                      <>
                        <strong className="font-semibold text-gray-700">
                          {planTitle}
                        </strong>
                        을 삭제하면 참여 중인{" "}
                        <strong className="font-semibold text-gray-700">
                          멤버 {members.length}명
                        </strong>
                        이 더 이상 계획에 접근할 수 없습니다. 이
                        작업은 되돌릴 수 없습니다.
                      </>
                  ) : (
                      <>
                        <strong className="font-semibold text-gray-700">
                          {planTitle}
                        </strong>
                        에서 나가면 더 이상 이 계획에 접근할 수
                        없습니다. 다시 참여하려면 초대 링크가
                        필요합니다.
                      </>
                  )}
                </p>

                {actionError && (
                    <p className="mt-3 rounded-xl bg-red-soft px-3 py-2.5 text-[12px] text-red">
                      {actionError}
                    </p>
                )}

                <div className="mt-5 flex flex-col gap-2">
                  <Button
                      variant="danger"
                      onClick={handleConfirm}
                      disabled={isSubmitting}
                  >
                    {isSubmitting
                        ? "처리 중..."
                        : confirmAction === "delete"
                            ? "계획 삭제하기"
                            : "계획 나가기"}
                  </Button>

                  <Button
                      variant="ghost"
                      onClick={closeConfirm}
                      disabled={isSubmitting}
                  >
                    취소
                  </Button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}