export function EmptyState({ emoji = "🧭", title, description }: { emoji?: string; title: string; description?: string }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-gray-300 px-6 py-10 text-center">
      <div className="text-3xl">{emoji}</div>
      <div className="text-[14.5px] font-bold text-ink">{title}</div>
      {description && <div className="text-[12.5px] leading-relaxed text-gray-500">{description}</div>}
    </div>
  );
}
