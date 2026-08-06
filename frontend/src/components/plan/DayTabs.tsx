import { cx } from "@/lib/utils";

export function DayTabs({
  days,
  active,
  onChange,
}: {
  days: Array<{ day: number; dateLabel: string }>;
  active: number;
  onChange: (day: number) => void;
}) {
  return (
    <div className="flex gap-1.5">
      {days.map((d) => {
        const pressed = d.day === active;
        return (
          <button
            key={d.day}
            type="button"
            aria-pressed={pressed}
            onClick={() => onChange(d.day)}
            className={cx(
              "flex-1 rounded-xl border px-1 py-2 text-center text-[12px] font-semibold leading-tight",
              pressed ? "border-primary bg-primary-soft text-primary-dark" : "border-gray-200 bg-white text-gray-700",
            )}
          >
            <strong className="block text-[13px]">Day{d.day}</strong>
            {d.dateLabel}
          </button>
        );
      })}
    </div>
  );
}
