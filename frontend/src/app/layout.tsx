import type { Metadata, Viewport } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import Script from "next/script";

export const metadata: Metadata = {
    title: "모이Go — 같이 짜는 여행 일정",
    description: "여러 명이 하나의 여행 계획에 참여해 장소를 찾고, 일정을 만들고, 투표로 결정하는 서비스",
};

export const viewport: Viewport = {
    width: "device-width",
    initialScale: 1,
    themeColor: "#3182F6",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="ko" className="h-full">
        <body className="min-h-full">
        <Script
            src="https://t1.kakaocdn.net/kakao_js_sdk/2.7.4/kakao.min.js"
            integrity="sha384-DKYJZ8NLiK8MN4/C5P2dtwHfp1jNhXbCFYaG6oTgh6VOQ3F4XkmQpmSAf9G7DqjD"
            crossOrigin="anonymous"
            strategy="beforeInteractive"
        />
        <AuthProvider>
            <div className="app-shell">{children}</div>
        </AuthProvider>
        </body>
        </html>
    );
}