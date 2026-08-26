declare global {
    interface Window {
        Kakao: any;
    }
}

let initialized = false;

function initKakao() {
    if (initialized || typeof window === "undefined" || !window.Kakao) return;
    window.Kakao.init(process.env.NEXT_PUBLIC_KAKAO_JS_KEY);
    initialized = true;
}

export function shareInviteToKakao(planTitle: string, inviteUrl: string) {
    initKakao();

    if (!window.Kakao?.isInitialized()) {
        alert("카카오톡 공유를 준비하지 못했습니다. 잠시 후 다시 시도해주세요.");
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