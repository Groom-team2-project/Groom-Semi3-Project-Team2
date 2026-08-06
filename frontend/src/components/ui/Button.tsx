import Link from "next/link";
import { cx } from "@/lib/utils";
import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "ghost" | "soft" | "kakao" | "danger";
type Size = "md" | "sm";

const VARIANT_CLASS: Record<Variant, string> = {
  primary: "bg-primary text-white",
  ghost: "bg-white border border-gray-200 text-ink",
  soft: "bg-primary-soft text-primary-dark",
  kakao: "bg-kakao text-kakao-ink",
  danger: "bg-red-soft text-red",
};

const SIZE_CLASS: Record<Size, string> = {
  md: "py-3.5 px-4 text-[15px] rounded-2xl",
  sm: "py-2.5 px-3.5 text-[13px] rounded-xl",
};

interface CommonProps {
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
  className?: string;
  children: React.ReactNode;
}

type ButtonProps = CommonProps &
  ButtonHTMLAttributes<HTMLButtonElement> & {
    href?: undefined;
  };

type LinkButtonProps = CommonProps & {
  href: string;
  disabled?: boolean;
};

const base = "appearance-none border-0 font-bold text-center cursor-pointer inline-flex items-center justify-center gap-1.5 transition-opacity active:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed";

export function Button({
  variant = "primary",
  size = "md",
  fullWidth = true,
  className,
  children,
  href,
  ...rest
}: ButtonProps | LinkButtonProps) {
  const classes = cx(base, VARIANT_CLASS[variant], SIZE_CLASS[size], fullWidth && "w-full", className);

  if (href) {
    return (
      <Link href={href} className={classes}>
        {children}
      </Link>
    );
  }

  return (
    <button type="button" className={classes} {...(rest as ButtonHTMLAttributes<HTMLButtonElement>)}>
      {children}
    </button>
  );
}
