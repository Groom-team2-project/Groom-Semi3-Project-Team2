import Link from "next/link";
import { cx } from "@/lib/utils";

interface CardProps {
  children: React.ReactNode;
  className?: string;
  href?: string;
  onClick?: () => void;
}

const base = "w-full flex flex-col gap-2 rounded-2xl border border-gray-200 bg-white p-3.5 text-left font-sans text-[14px] text-ink transition-shadow";

export function Card({ children, className, href, onClick }: CardProps) {
  const classes = cx(base, (href || onClick) && "cursor-pointer active:shadow-sm hover:border-gray-300", className);
  if (href) {
    return (
      <Link href={href} className={classes}>
        {children}
      </Link>
    );
  }
  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={classes}>
        {children}
      </button>
    );
  }
  return <div className={classes}>{children}</div>;
}
