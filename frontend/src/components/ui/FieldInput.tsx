import { cx } from "@/lib/utils";
import type { InputHTMLAttributes, TextareaHTMLAttributes } from "react";

const inputClass =
  "w-full rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-[14.5px] text-ink placeholder:text-gray-500 focus:border-primary focus:bg-white focus:outline-none";

export function Field({
  label,
  optional,
  children,
}: {
  label: string;
  optional?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-[12.5px] font-bold text-gray-700">
        {label} {optional && <span className="font-normal text-gray-500">(선택)</span>}
      </label>
      {children}
    </div>
  );
}

export function FieldInput({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx(inputClass, className)} {...rest} />;
}

const HOURS = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, "0"));
const MINUTES = Array.from({ length: 12 }, (_, index) => String(index * 5).padStart(2, "0"));

export function TimeFieldInput({
  value,
  onChange,
  optional = false,
}: {
  value: string;
  onChange: (value: string) => void;
  optional?: boolean;
}) {
  const [selectedHour = "", selectedMinute = ""] = value.split(":");
  const minute = MINUTES.includes(selectedMinute) ? selectedMinute : "";

  return (
    <div className="grid grid-cols-2 gap-2">
      <select
        aria-label="시 선택"
        value={selectedHour}
        onChange={(event) => {
          const hour = event.target.value;
          onChange(hour ? `${hour}:${minute || "00"}` : "");
        }}
        className={cx(inputClass, "font-mono")}
      >
        {optional && <option value="">설정 안 함</option>}
        {HOURS.map((hour) => <option key={hour} value={hour}>{hour}시</option>)}
      </select>
      <select
        aria-label="분 선택"
        value={minute}
        disabled={!selectedHour}
        onChange={(event) => onChange(`${selectedHour}:${event.target.value}`)}
        className={cx(inputClass, "font-mono disabled:cursor-not-allowed disabled:opacity-50")}
      >
        {!minute && <option value="">분 선택</option>}
        {MINUTES.map((item) => <option key={item} value={item}>{item}분</option>)}
      </select>
    </div>
  );
}

export function FieldTextarea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cx(inputClass, "min-h-[84px] resize-none", className)} {...rest} />;
}
