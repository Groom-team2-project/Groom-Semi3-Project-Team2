import { cx } from "@/lib/utils";

const SIZE_PX: Record<"xs" | "sm" | "md" | "lg", number> = {
  xs: 24,
  sm: 28,
  md: 32,
  lg: 52,
};

export function Avatar({
  name,
  color,
  size = "sm",
  className,
  overlap = false,
  label,
}: {
  name: string;
  color: string;
  size?: "xs" | "sm" | "md" | "lg";
  className?: string;
  overlap?: boolean;
  /** 아바타 안에 표시할 텍스트를 이니셜 대신 지정 (예: "+1") */
  label?: string;
}) {
  const px = SIZE_PX[size];
  return (
    <span
      className={cx(
        "inline-flex items-center justify-center rounded-full font-extrabold text-white shrink-0 border-2 border-white",
        overlap && "-ml-2 first:ml-0",
        className,
      )}
      style={{ width: px, height: px, background: color, fontSize: label ? px * 0.3 : px * 0.36 }}
    >
      {label ?? name.slice(0, 1)}
    </span>
  );
}

export function AvatarStack({ names, colors }: { names: string[]; colors: string[] }) {
  return (
    <div className="flex">
      {names.slice(0, 3).map((n, i) => (
        <Avatar key={i} name={n} color={colors[i]} size="xs" overlap />
      ))}
      {names.length > 3 && (
        <Avatar name="+" label={`+${names.length - 3}`} color="#8B7FF2" size="xs" overlap />
      )}
    </div>
  );
}
