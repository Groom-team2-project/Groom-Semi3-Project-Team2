"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
    getInvitationByCode,
    joinByInviteCode,
} from "@/lib/api/invitations";
import type { Invitation } from "@/lib/api/types";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/context/AuthContext";
import { savePostLoginRedirect } from "@/lib/postLoginRedirect";

export default function InvitationPage() {
    const params = useParams();
    const router = useRouter();
    const { loginWithKakao } = useAuth();

    const inviteCode = params.inviteCode as string;

    const [invitation, setInvitation] = useState<Invitation | null>(null);
    const [loading, setLoading] = useState(true);
    const [joining, setJoining] = useState(false);
    const [loginPending, setLoginPending] = useState(false);
    const [requiresLogin, setRequiresLogin] = useState(false);
    const [loginError, setLoginError] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [alreadyJoined, setAlreadyJoined] = useState(false);

    useEffect(() => {
        if (!inviteCode) {
            return;
        }

        const loadInvitation = async () => {
            try {
                const result = await getInvitationByCode(inviteCode);
                setInvitation(result);
            } catch (e) {
                // 미로그인 상태라면 로그인 페이지로 바로 보내지 않고
                // 현재 초대 페이지에서 로그인 안내 화면을 보여줍니다.
                if (
                    e instanceof ApiError &&
                    (e.status === 401 || e.errorCode === "UNAUTHORIZED")
                ) {
                    setRequiresLogin(true);
                    return;
                }

                setError(
                    e instanceof ApiError
                      ? e.message
                      : "초대 정보를 불러오지 못했습니다.",
                );
            } finally {
                setLoading(false);
            }
        };

        void loadInvitation();
    }, [inviteCode]);

    const handleLogin = async () => {
        if (loginPending) {
            return;
        }

        setLoginPending(true);
        setLoginError(null);

        if (!savePostLoginRedirect(`/invitations/${inviteCode}`)) {
            setLoginError(
                "브라우저 저장소를 사용할 수 없습니다. 저장소 접근을 허용한 뒤 다시 시도해 주세요.",
            );
            setLoginPending(false);
            return;
        }

        // 로그인 페이지를 거치지 않고 바로 카카오 로그인 실행
        try {
            await loginWithKakao();
        } catch (e) {
            setLoginError(
                e instanceof ApiError
                  ? e.message
                  : "카카오 로그인을 시작하지 못했습니다. 다시 시도해 주세요.",
            );
            setLoginPending(false);
        }
    };

    const handleJoin = async () => {
        if (!inviteCode || joining) {
            return;
        }

        try {
            setJoining(true);
            setError(null);

            const result = await joinByInviteCode(inviteCode);

            router.replace(`/plans/${result.planId}`);
        } catch (e) {
            // 초대 화면에 머무는 동안 로그인이 만료된 경우
            // 로그인 안내 화면으로 전환합니다.
            if (
                e instanceof ApiError &&
                (e.status === 401 || e.errorCode === "UNAUTHORIZED")
            ) {
                setRequiresLogin(true);
                return;
            }

            // 이미 참여 중인 계획
            if (
                e instanceof ApiError &&
                e.errorCode === "MEMBER_ALREADY_JOINED"
            ) {
                setAlreadyJoined(true);
                return;
            }

            setError(
                e instanceof ApiError
                  ? e.message
                  : "계획 참여 중 오류가 발생했습니다.",
            );
        } finally {
            setJoining(false);
        }
    };

    // 초대 코드가 없는 경우
    if (!inviteCode) {
        return (
            <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4 py-12">
                <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">
                    <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-50 text-xl font-bold text-red-500">
                        !
                    </div>

                    <p className="text-sm font-semibold text-gray-400">
                        MOIGO 초대
                    </p>

                    <h1 className="mt-2 text-xl font-bold text-gray-900">
                        초대 링크를 확인해주세요
                    </h1>

                    <p className="mt-3 text-sm leading-6 text-gray-500">
                        초대 코드를 확인할 수 없습니다.
                    </p>
                </div>
            </main>
        );
    }

    // 초대 정보 확인 중
    if (loading) {
        return (
            <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4">
                <div className="text-center">
                    <p className="text-sm text-gray-500">
                        초대 정보를 확인하고 있습니다.
                    </p>
                </div>
            </main>
        );
    }

    // 미로그인 화면
    if (requiresLogin) {
        return (
            <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4 py-12">
                <div className="w-full max-w-md overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
                    <div className="px-7 py-8 text-center">

                        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-2xl">
                            ✉
                        </div>

                        <p className="text-sm font-semibold text-emerald-600">
                            MOIGO 초대
                        </p>

                        <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900">
                            계획에 초대받았어요
                        </h1>

                        <p className="mt-3 text-sm leading-6 text-gray-500">
                            이 초대에 참여하려면 로그인이 필요합니다.
                            <br />
                            로그인 후 초대 페이지로 다시 돌아옵니다.
                        </p>

                        <div className="mt-6 rounded-xl bg-gray-50 px-4 py-4 text-left">
                            <p className="text-xs font-medium text-gray-500">
                                초대 코드
                            </p>

                            <p className="mt-1 font-mono text-base font-semibold tracking-wider text-gray-900">
                                {inviteCode}
                            </p>
                        </div>

                        <button
                            type="button"
                            onClick={handleLogin}
                            disabled={loginPending}
                            className="mt-6 w-full rounded-xl bg-[#FEE500] px-4 py-3.5 text-sm font-semibold text-[#191919] transition hover:brightness-95 active:brightness-90 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {loginPending
                                ? "로그인 준비 중..."
                                : "카카오로 로그인하고 참여하기"}
                        </button>

                        {loginError && (
                            <div className="mt-3 rounded-xl bg-red-soft px-4 py-3 text-left">
                                <p className="text-sm text-red">{loginError}</p>
                            </div>
                        )}

                        <p className="mt-3 text-xs text-gray-400">
                            카카오 로그인만 지원합니다.
                        </p>
                    </div>
                </div>
            </main>
        );
    }

    // 만료, 취소, 잘못된 코드 등 초대 오류 화면
    if (error && !invitation) {
        return (
            <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4 py-12">
                <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 text-center shadow-sm">

                    <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-50 text-xl font-bold text-red-500">
                        !
                    </div>

                    <p className="text-sm font-semibold text-gray-400">
                        MOIGO 초대
                    </p>

                    <h1 className="mt-2 text-xl font-bold text-gray-900">
                        초대 링크를 확인해주세요
                    </h1>

                    <p className="mt-3 text-sm leading-6 text-gray-500">
                        {error}
                    </p>

                    <button
                        type="button"
                        onClick={() => router.push("/")}
                        className="mt-6 w-full rounded-xl bg-slate-900 px-4 py-3.5 text-sm font-semibold text-white transition hover:bg-slate-800"
                    >
                        홈으로 이동
                    </button>
                </div>
            </main>
        );
    }
// 이미 참여 중인 계획
    if (alreadyJoined && invitation?.planId) {
        return (
            <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4 py-12">
                <div className="w-full max-w-md overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
                    <div className="px-7 py-8 text-center">

                        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-2xl font-bold text-emerald-600">
                            ✓
                        </div>

                        <p className="text-sm font-semibold text-emerald-600">
                            MOIGO 초대
                        </p>

                        <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900">
                            이미 참여 중인 계획입니다
                        </h1>

                        <p className="mt-3 text-sm leading-6 text-gray-500">
                            이미 이 계획의 멤버로 참여하고 있습니다.
                        </p>

                        <button
                            type="button"
                            onClick={() =>
                                router.replace(`/plans/${invitation.planId}`)
                            }
                            className="mt-6 w-full rounded-xl bg-emerald-600 px-4 py-3.5 text-sm font-semibold text-white transition hover:bg-emerald-700"
                        >
                            계획으로 바로가기
                        </button>
                    </div>
                </div>
            </main>
        );
    }

    // 로그인된 사용자 - 정상 초대 화면
    return (
        <main className="flex min-h-[calc(100vh-64px)] items-center justify-center bg-[#f8faf9] px-4 py-12">
            <div className="w-full max-w-md overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
                <div className="px-7 py-8">

                    <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-50 text-2xl">
                        ✉
                    </div>

                    <p className="text-sm font-semibold text-emerald-600">
                        MOIGO 초대
                    </p>

                    <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900">
                        계획에 초대받았어요
                    </h1>

                    <p className="mt-3 text-sm leading-6 text-gray-500">
                        초대를 수락하면 계획의 멤버로 참여할 수 있습니다.
                    </p>

                    <div className="mt-6 rounded-xl bg-gray-50 px-4 py-4">
                        <p className="text-xs font-medium text-gray-500">
                            초대 코드
                        </p>

                        <p className="mt-1 font-mono text-base font-semibold tracking-wider text-gray-900">
                            {inviteCode}
                        </p>
                    </div>

                    {invitation && (
                        <div className="mt-4 rounded-xl border border-gray-200 px-4 py-4">
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-gray-500">
                                    초대 상태
                                </span>

                                {invitation.status === "ACTIVE" ? (
                                    <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                                        참여 가능
                                    </span>
                                ) : invitation.status === "EXPIRED" ? (
                                    <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-500">
                                        만료됨
                                    </span>
                                ) : (
                                    <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-500">
                                        취소됨
                                    </span>
                                )}
                            </div>
                        </div>
                    )}

                    {error && (
                        <div className="mt-4 rounded-xl bg-red-50 px-4 py-3">
                            <p className="text-sm text-red-500">
                                {error}
                            </p>
                        </div>
                    )}

                    <button
                        type="button"
                        onClick={handleJoin}
                        disabled={
                            joining ||
                            invitation?.status !== "ACTIVE"
                        }
                        className="mt-6 w-full rounded-xl bg-emerald-600 px-4 py-3.5 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {joining
                            ? "참여 중..."
                            : "계획 참여하기"}
                    </button>

                    <button
                        type="button"
                        onClick={() => router.push("/")}
                        className="mt-3 w-full rounded-xl border border-gray-200 bg-white px-4 py-3.5 text-sm font-semibold text-gray-600 transition hover:bg-gray-50"
                    >
                        나중에 참여하기
                    </button>
                </div>
            </div>
        </main>
    );
}
