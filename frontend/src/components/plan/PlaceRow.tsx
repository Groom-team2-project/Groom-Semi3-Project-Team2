import { Tag } from "@/components/ui/Tag";

export function PlaceRow({
  emoji,
  name,
  address,
  tag,
  onClick,
  onAdd,
  onRemove,
  addDisabled = false,
}: {
  emoji: string;
  name: string;
  address?: string;
  tag?: { label: string; color: "blue" | "gray" | "orange" };
  onClick?: () => void;
  onAdd?: () => void;
  onRemove?: () => void;
  addDisabled?: boolean;
}) {
  const details = (
    <>
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[10px] bg-gray-100 text-lg">{emoji}</div>
      <div className="min-w-0 flex-1">
        <div className="truncate text-[14.5px] font-bold">{name}</div>
        {address && <div className="truncate text-[12px] leading-relaxed text-gray-500">{address}</div>}
      </div>
    </>
  );
  const content = (
    <>
      {onClick ? (
        <button type="button" onClick={onClick} aria-label={name + " 지도에서 보기"}
          className="flex min-w-0 flex-1 items-start gap-3 text-left text-inherit">
          {details}
        </button>
      ) : <div className="flex min-w-0 flex-1 items-start gap-3">{details}</div>}
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
      {onRemove && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          aria-label="삭제"
          className="flex h-[30px] w-[30px] shrink-0 items-center justify-center rounded-[9px] border border-gray-200 bg-white text-[13px] font-bold text-gray-500"
        >
          ×
        </button>
      )}
    </>
  );

  return <div className="flex w-full items-start gap-3 border-b border-gray-200 py-3 text-left">{content}</div>;
}
