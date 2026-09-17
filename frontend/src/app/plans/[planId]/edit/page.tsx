"use client";

import { use, useState } from "react";
import { useRouter } from "next/navigation";
import { AppBar } from "@/components/ui/AppBar";
import { Button } from "@/components/ui/Button";
import {
    Field,
    FieldInput,
    FieldTextarea,
} from "@/components/ui/FieldInput";
import { PlanNotFound } from "@/components/plan/PlanNotFound";
import { usePlan } from "@/lib/hooks/usePlan";
import { updatePlan } from "@/lib/api";

export default function PlanEditPage({
                                         params,
                                     }: {
    params: Promise<{ planId: string }>;
}) {
    const { planId } = use(params);
    const { plan, isLoading } = usePlan(planId);

    if (isLoading) {
        return null;
    }

    if (!plan) {
        return <PlanNotFound />;
    }

    if (plan.myRole !== "OWNER" && plan.myRole !== "EDITOR") {
        return <PlanNotFound />;
    }

    return (
        <PlanEditForm
            key={planId}
            planId={planId}
            initialTitle={plan.title}
            initialStartDate={plan.startDate}
            initialEndDate={plan.endDate}
            initialDescription={plan.description ?? ""}
            initialCapacity={plan.capacity ? String(plan.capacity) : ""}
        />
    );
}

function PlanEditForm({
                          planId,
                          initialTitle,
                          initialStartDate,
                          initialEndDate,
                          initialDescription,
                          initialCapacity,
                      }: {
    planId: string;
    initialTitle: string;
    initialStartDate: string;
    initialEndDate: string;
    initialDescription: string;
    initialCapacity: string;
}) {
    const router = useRouter();

    const [title, setTitle] = useState(initialTitle);
    const [startDate, setStartDate] = useState(initialStartDate);
    const [endDate, setEndDate] = useState(initialEndDate);
    const [description, setDescription] = useState(initialDescription);
    const [capacity, setCapacity] = useState(initialCapacity);
    const [pending, setPending] = useState(false);

    const capacityNumber = Number(capacity);

    const isDateValid =
        Boolean(startDate) &&
        Boolean(endDate) &&
        endDate >= startDate;

    const isCapacityValid =
        capacity === "" ||
        (
            Number.isInteger(capacityNumber) &&
            capacityNumber >= 1
        );

    const canSubmit =
        title.trim().length > 0 &&
        isDateValid &&
        isCapacityValid &&
        !pending;
    const [errorMessage, setErrorMessage] = useState("");

    async function handleSubmit() {
        if (!canSubmit) {
            return;
        }

        setPending(true);
        setErrorMessage("");

        try {
            await updatePlan(planId, {
                title: title.trim(),
                description: description.trim() || undefined,
                startDate,
                endDate,
                capacity: capacity ? Number(capacity) : undefined,
            });

            router.push(`/plans/${planId}`);
        } catch (error) {
            setErrorMessage(
                error instanceof Error
                    ? error.message
                    : "계획을 수정하지 못했습니다.",
            );
        } finally {
            setPending(false);
        }
    }
    return (
        <div className="flex min-h-dvh flex-col">
            <AppBar
                title="여행 계획 수정"
                backHref={`/plans/${planId}`}
            />

            <div className="flex flex-1 flex-col gap-3 px-4 pb-8">
                <Field label="제목">
                    <FieldInput
                        placeholder="ex) 제주도 여름 여행"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                    />
                </Field>

                <Field label="여행 기간">
                    <div className="flex items-center gap-2">
                        <FieldInput
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                        />

                        <span className="text-gray-500">-</span>

                        <FieldInput
                            type="date"
                            value={endDate}
                            min={startDate}
                            onChange={(e) => setEndDate(e.target.value)}
                        />
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

                {errorMessage && (
                    <p
                        role="alert"
                        className="text-[13px] text-red"
                    >
                        {errorMessage}
                    </p>
                )}

                <Button
                    onClick={handleSubmit}
                    disabled={!canSubmit}
                >
                    {pending ? "수정하는 중..." : "수정 완료"}
                </Button>
            </div>
        </div>
    );
}