interface KakaoSdk {
    init: (key: string) => void;
    isInitialized: () => boolean;
    Share: {
        sendDefault: (options: Record<string, unknown>) => void;
    };
}

declare global {
    interface Window {
        Kakao?: KakaoSdk;
    }
}

let initialized = false;

function initKakao() {
    if (
        initialized ||
        typeof window === "undefined" ||
        !window.Kakao
    ) {
        return;
    }

    const kakaoJsKey = process.env.NEXT_PUBLIC_KAKAO_JS_KEY;

    if (!kakaoJsKey) {
        console.error("NEXT_PUBLIC_KAKAO_JS_KEY가 설정되지 않았습니다.");
        return;
    }

    if (!window.Kakao.isInitialized()) {
        window.Kakao.init(kakaoJsKey);
    }

    initialized = true;
}

export function shareInviteToKakao(
    planTitle: string,
    inviteUrl: string,
) {
    initKakao();

    if (!window.Kakao?.isInitialized()) {
        alert(
            "카카오톡 공유를 준비하지 못했습니다. 잠시 후 다시 시도해주세요.",
        );
        return;
    }

    window.Kakao.Share.sendDefault({
        objectType: "feed",
        content: {
            title: `${planTitle} 여행에 초대합니다`,
            description: "링크를 눌러 함께 여행을 계획해요!",
            imageUrl: "https://moigo.netlify.app/og-image.png",
            link: {
                mobileWebUrl: inviteUrl,
                webUrl: inviteUrl,
            },
        },
        buttons: [
            {
                title: "초대 링크 열기",
                link: {
                    mobileWebUrl: inviteUrl,
                    webUrl: inviteUrl,
                },
            },
        ],
    });
}