"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cx } from "@/lib/utils";

export function BottomTabBar({ planId }: { planId: string | null }) {
  const pathname = usePathname();

  const tabs = [
    { key: "home", label: "홈", icon: "🏠", href: "/plans" },
    { key: "timeline", label: "일정", icon: "🗓", href: planId ? `/plans/${planId}/timeline` : "/plans" },
    { key: "votes", label: "투표", icon: "🗳️", href: planId ? `/plans/${planId}/votes` : "/plans" },
    { key: "profile", label: "내정보", icon: "👤", href: "/profile" },
  ];

  return (
    <nav
      aria-label="주요 화면"
      className="sticky bottom-0 z-10 grid flex-shrink-0 grid-cols-4 border-t border-gray-200 bg-white/95 backdrop-blur"
    >
      {tabs.map((tab) => {
        const active =
          tab.key === "home"
            ? pathname === "/plans" || (planId != null && pathname === `/plans/${planId}`)
            : pathname === tab.href || (pathname.startsWith(tab.href) && tab.href !== "/plans");
        return (
          <Link
            key={tab.key}
            href={tab.href}
            className={cx(
              "flex flex-col items-center gap-0.5 py-2.5 pb-3 text-[10.5px] font-semibold",
              active ? "text-primary" : "text-gray-500",
            )}
          >
            <span className="text-[17px] leading-none">{tab.icon}</span>
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
