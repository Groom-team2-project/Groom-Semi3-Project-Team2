import { cx } from "@/lib/utils";

export function Segmented<T extends string>({
  options,
  value,
  onChange,
}: {
  options: Array<{ value: T; label: string }>;
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <div className="flex gap-1.5 overflow-x-auto pb-0.5">
      {options.map((opt) => {
        const pressed = opt.value === value;
        return (
          <button
            key={opt.value}
            type="button"
            aria-pressed={pressed}
            onClick={() => onChange(opt.value)}
            className={cx(
              "whitespace-nowrap rounded-full border px-3.5 py-2 text-[13px] font-semibold",
              pressed ? "border-ink bg-ink text-white" : "border-gray-200 bg-white text-gray-700",
            )}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
