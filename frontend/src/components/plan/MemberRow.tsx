"use client";

import { Avatar } from "@/components/ui/Avatar";
import { cx } from "@/lib/utils";
import type { Member, Role } from "@/lib/api";

const ROLE_LABEL: Record<Role, string> = { OWNER: "모임장", EDITOR: "편집자", VIEWER: "뷰어" };

export function MemberRow({
  member,
  editable,
  onChangeRole,
}: {
  member: Member;
  editable?: boolean;
  onChangeRole?: (role: Role) => void;
}) {
  return (
    <div className="flex items-center gap-2.5 border-b border-gray-200 py-2.5 text-[14.5px] font-semibold">
      <Avatar name={member.name} color={member.avatarColor} size="xs" />
      <span>{member.name}</span>
      <div className="flex-1" />
      {editable && member.role !== "OWNER" ? (
        <select
          value={member.role}
          onChange={(e) => onChangeRole?.(e.target.value as Role)}
          className="cursor-pointer rounded-full border-0 bg-gray-100 px-2.5 py-1.5 text-[11px] font-bold text-gray-700"
        >
          <option value="EDITOR">편집자 ▾</option>
          <option value="VIEWER">뷰어 ▾</option>
        </select>
      ) : (
        <span
          className={cx(
            "rounded-full px-2.5 py-1.5 text-[11px] font-bold",
            member.role === "OWNER" ? "bg-primary-soft text-primary-dark" : "border border-gray-200 bg-white text-gray-500",
          )}
        >
          {ROLE_LABEL[member.role]}
        </span>
      )}
    </div>
  );
}
