"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import { Field, FieldInput, FieldTextarea } from "@/components/ui/FieldInput";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { updatePlan } from "@/lib/api";

export default function PlanEditPage({
                                         params,
                                     }: {
    params: Promise<{ planId: string }>;
}) {
    const { planId } = use(params);
    const router = useRouter();
    const { plan, isLoading } = usePlan(planId);

    const [title, setTitle] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [description, setDescription] = useState("");
    const [capacity, setCapacity] = useState("");
    const [pending, setPending] = useState(false);

    useEffect(() => {
        if (!plan) {
            return;
        }
        setTitle(plan.title);
        setStartDate(plan.startDate);
        setEndDate(plan.endDate);
        setDescription(plan.description ?? "");
        setCapacity(plan.capacity ? String(plan.capacity) : "");
    }, [plan]);

    if (isLoading) {
        return null;
    }

    if (!plan) {
        return <PlanNotFound />;
    }

    if (plan.myRole !== "OWNER" && plan.myRole !== "EDITOR") {
        return <PlanNotFound />;
    }

    const canSubmit = title.trim().length > 0 && startDate && endDate && !pending;

    async function handleSubmit() {
        if (!canSubmit) return;
        setPending(true);
        try {
            await updatePlan(planId, {
                title: title.trim(),
                description: description.trim() || undefined,
                startDate,
                endDate,
                capacity: capacity ? Number(capacity) : undefined,
            });
            router.push(`/plans/${planId}`);
        } finally {
            setPending(false);
        }
    }

    return (
        <div className="flex min-h-dvh flex-col">
            <AppBar title="여행 계획 수정" backHref={`/plans/${planId}`} />
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
                    {pending ? "수정하는 중..." : "수정 완료"}
                </Button>
            </div>
        </div>
    );
}