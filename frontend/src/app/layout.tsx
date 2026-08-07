import type { Metadata, Viewport } from "next";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";

export const metadata: Metadata = {
  title: "트립메이트 — 같이 짜는 여행 일정",
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
        <AuthProvider>
          <div className="app-shell">{children}</div>
        </AuthProvider>
      </body>
    </html>
  );
}
