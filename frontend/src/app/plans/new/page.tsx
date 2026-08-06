"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput, FieldTextarea } from "@/components/ui/FieldInput";
import { createPlan } from "@/lib/api";
import { todayISO } from "@/lib/utils";

export default function PlanCreatePage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [startDate, setStartDate] = useState(todayISO());
  const [endDate, setEndDate] = useState(todayISO());
  const [description, setDescription] = useState("");
  const [capacity, setCapacity] = useState("");
  const [pending, setPending] = useState(false);

  const canSubmit = title.trim().length > 0 && startDate && endDate && !pending;

  async function handleSubmit() {
    if (!canSubmit) return;
    setPending(true);
    try {
      const plan = await createPlan({
        title: title.trim(),
        description: description.trim() || undefined,
        startDate,
        endDate,
        capacity: capacity ? Number(capacity) : undefined,
      });
      router.push(`/plans/${plan.id}`);
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar title="새 여행 계획 만들기" backHref="/plans" />
      <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
        <Field label="제목">
          <FieldInput placeholder="ex) 제주도 여름 여행" value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="여행 기간">
          <div className="flex items-center gap-2">
            <FieldInput type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            <span className="text-gray-500">-</span>
            <FieldInput type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
          </div>
        </Field>
        <Field label="설명">
          <FieldTextarea
            placeholder="간단한 소개를 적어주세요"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </Field>
        <Field label="모집 인원" optional>
          <FieldInput
            type="number"
            min={1}
            placeholder="ex) 4명"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value)}
          />
        </Field>
        <div className="h-1.5" />
        <Button onClick={handleSubmit} disabled={!canSubmit}>
          {pending ? "만드는 중..." : "여행 계획 만들기"}
        </Button>
      </div>
    </div>
  );
}
