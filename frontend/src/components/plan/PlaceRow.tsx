import { Tag } from "@/components/ui/Tag";

export function PlaceRow({
  emoji,
  name,
  address,
  tag,
  onClick,
  onAdd,
  addDisabled = false,
}: {
  emoji: string;
  name: string;
  address?: string;
  tag?: { label: string; color: "blue" | "gray" | "orange" };
  onClick?: () => void;
  onAdd?: () => void;
  addDisabled?: boolean;
}) {
  const content = (
    <>
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[10px] bg-gray-100 text-lg">{emoji}</div>
      <div className="min-w-0 flex-1">
        <div className="truncate text-[14.5px] font-bold">{name}</div>
        {address && <div className="truncate text-[12px] leading-relaxed text-gray-500">{address}</div>}
      </div>
      {tag && <Tag color={tag.color}>{tag.label}</Tag>}
      {onAdd && (
        <button
          type="button"
          disabled={addDisabled}
          onClick={(e) => {
            e.stopPropagation();
            onAdd();
          }}
          aria-label="추가"
          className="flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-[9px] border border-gray-200 bg-white text-[17px] font-bold text-primary disabled:cursor-not-allowed disabled:opacity-50"
        >
          +
        </button>
      )}
    </>
  );

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        className="flex w-full items-start gap-3 border-b border-gray-200 py-3 text-left font-sans text-inherit"
      >
        {content}
      </button>
    );
  }
  return <div className="flex w-full items-start gap-3 border-b border-gray-200 py-3 text-left">{content}</div>;
}
