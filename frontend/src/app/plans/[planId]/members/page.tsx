"use client";

import { use, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/FieldInput";
import { MemberRow } from "@/components/plan/MemberRow";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { getInvitation, getMembers, updateMemberRole } from "@/lib/api";
import type { Invitation, Member, Role } from "@/lib/api";

export default function MembersPage({ params }: { params: Promise<{ planId: string }> }) {
  const { planId } = use(params);
  const { plan, isLoading } = usePlan(planId);
  const [invitation, setInvitation] = useState<Invitation | null>(null);
  const [copied, setCopied] = useState(false);
  const [members, setMembers] = useState<Member[]>([]);

  useEffect(() => {
    getInvitation(planId).then(setInvitation);
    getMembers(planId).then(setMembers);
  }, [planId]);

  if (isLoading) return null;
  if (!plan) return <PlanNotFound />;

  const isOwner = plan.myRole === "OWNER";

  async function handleCopy() {
    if (!invitation) return;
    try {
      await navigator.clipboard.writeText(invitation.url);
    } catch {
      // 클립보드 권한이 없는 환경일 수 있음 — 무시
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  async function handleRoleChange(memberId: string, role: Role) {
    await updateMemberRole(planId, memberId, role);
    const updatedMembers = await getMembers(planId);
    setMembers(updatedMembers);
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="멤버 관리" backHref={`/plans/${planId}`} />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="초대 링크">
          <div className="flex items-center gap-2 rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 font-mono text-[12px] text-gray-700">
            <span className="truncate">{invitation?.url ?? "링크 생성 중..."}</span>
            <span className="flex-1" />
            <button type="button" onClick={handleCopy} className="shrink-0 font-bold text-primary">
              {copied ? "복사됨" : "복사"}
            </button>
          </div>
        </Field>
        <Button variant="kakao">카카오톡으로 초대하기</Button>

        <h3 className="mt-1 text-[13px] text-gray-500">참여 멤버 ({members.length}명)</h3>
        <div>
          {members.map((m) => (
            <MemberRow
              key={m.id}
              member={m}
              editable={isOwner}
              onChangeRole={(role) => handleRoleChange(m.id, role)}
            />
          ))}
        </div>
        <p className="text-[12px] leading-relaxed text-gray-500">
          초대 링크로 들어오면 승인 절차 없이 즉시 편집자로 참여됩니다. 역할 태그를 누르면 모임장이 개별 멤버를 편집자 ↔
          뷰어로 전환할 수 있어요.
        </p>
      </div>
    </div>
  );
}
