import { cx } from "@/lib/utils";

type TagColor = "blue" | "gray" | "orange";

const COLOR_CLASS: Record<TagColor, string> = {
  blue: "bg-primary-soft text-primary-dark",
  gray: "bg-gray-100 text-gray-500",
  orange: "bg-orange-soft text-orange",
};

export function Tag({ color = "gray", children, className }: { color?: TagColor; children: React.ReactNode; className?: string }) {
  return (
    <span className={cx("inline-block whitespace-nowrap rounded-full px-2 py-1 text-[10.5px] font-bold", COLOR_CLASS[color], className)}>
      {children}
    </span>
  );
}

export function Chip({ tone = "default", children }: { tone?: "default" | "primary"; children: React.ReactNode }) {
  return (
    <span
      className={cx(
        "inline-block rounded-full px-3 py-1.5 text-[12.5px] font-semibold",
        tone === "primary" ? "bg-primary-soft text-primary-dark border border-transparent" : "bg-white text-gray-700 border border-gray-200",
      )}
    >
      {children}
    </span>
  );
}
