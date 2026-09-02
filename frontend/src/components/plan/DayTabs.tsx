"use client";

import { useEffect, useRef, useState } from "react";
import { cx } from "@/lib/utils";

const TAB_STEP_PX = 64 + 6;

export function DayTabs({
  days,
  active,
  onChange,
}: {
  days: Array<{ day: number; dateLabel: string }>;
  active: number;
  onChange: (day: number) => void;
}) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const activeButtonRef = useRef<HTMLButtonElement>(null);
  const dragOriginRef = useRef<{ pointerId: number; startX: number; startScrollLeft: number; captured: boolean } | null>(
    null,
  );
  const draggedRef = useRef(false);
  const [canScrollPrev, setCanScrollPrev] = useState(false);
  const [canScrollNext, setCanScrollNext] = useState(false);

  function updateScrollState() {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollPrev(el.scrollLeft > 4);
    setCanScrollNext(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
  }

  useEffect(() => {
    updateScrollState();
  }, [days.length]);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    el.addEventListener("scroll", updateScrollState, { passive: true });
    window.addEventListener("resize", updateScrollState);
    return () => {
      el.removeEventListener("scroll", updateScrollState);
      window.removeEventListener("resize", updateScrollState);
    };
  }, []);

  useEffect(() => {
    activeButtonRef.current?.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
  }, [active]);

  function handlePointerDown(event: React.PointerEvent<HTMLDivElement>) {
    const el = scrollRef.current;
    if (!el || event.pointerType === "touch") return;
    dragOriginRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startScrollLeft: el.scrollLeft,
      captured: false,
    };
    draggedRef.current = false;
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>) {
    const el = scrollRef.current;
    const origin = dragOriginRef.current;
    if (!el || !origin || origin.pointerId !== event.pointerId) return;
    const delta = event.clientX - origin.startX;

    if (!origin.captured) {
      // 아직 클릭 범위(6px 이내)면 스크롤도, 캡처도 건드리지 않는다.
      if (Math.abs(delta) <= 6) return;
      origin.captured = true;
      draggedRef.current = true;
      el.setPointerCapture(event.pointerId);
    }

    el.scrollLeft = origin.startScrollLeft - delta;
  }

  function endDrag(event: React.PointerEvent<HTMLDivElement>) {
    const el = scrollRef.current;
    const origin = dragOriginRef.current;
    if (el && origin?.captured && origin.pointerId === event.pointerId) {
      el.releasePointerCapture(event.pointerId);
    }
    dragOriginRef.current = null;
  }

  // 드래그가 끝난 직후 발생하는 click까지 탭 전환으로 처리되지 않도록 막는다.
  function handleTabClick(day: number) {
    if (draggedRef.current) {
      draggedRef.current = false;
      return;
    }
    onChange(day);
  }

  function scrollByTabs(count: number) {
    scrollRef.current?.scrollBy({ left: count * TAB_STEP_PX, behavior: "smooth" });
  }

  return (
    <div className="flex items-center gap-1">
      {canScrollPrev && (
        <button
          type="button"
          aria-label="이전 날짜"
          onClick={() => scrollByTabs(-3)}
          className="flex h-9 w-6 flex-none items-center justify-center rounded-lg border border-gray-200 bg-white text-gray-500 active:bg-gray-100"
        >
          ‹
        </button>
      )}
      <div
        ref={scrollRef}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        className="flex flex-1 cursor-grab gap-1.5 overflow-x-auto scroll-smooth pb-0.5 select-none active:cursor-grabbing"
      >
        {days.map((d) => {
          const pressed = d.day === active;
          return (
            <button
              key={d.day}
              ref={pressed ? activeButtonRef : undefined}
              type="button"
              aria-pressed={pressed}
              onClick={() => handleTabClick(d.day)}
              className={cx(
                // grow: 며칠 안 되면 지금처럼 폭을 꽉 채운다. shrink-0 + basis-16: 다 못 들어가면
                // 64px 밑으로는 안 줄어들고 그대로 넘쳐서 스크롤 컨테이너가 스크롤을 담당한다.
                "grow shrink-0 basis-16 rounded-xl border px-1 py-2 text-center text-[12px] font-semibold leading-tight",
                pressed ? "border-primary bg-primary-soft text-primary-dark" : "border-gray-200 bg-white text-gray-700",
              )}
            >
              <strong className="block text-[13px]">Day{d.day}</strong>
              {d.dateLabel}
            </button>
          );
        })}
      </div>
      {canScrollNext && (
        <button
          type="button"
          aria-label="다음 날짜"
          onClick={() => scrollByTabs(3)}
          className="flex h-9 w-6 flex-none items-center justify-center rounded-lg border border-gray-200 bg-white text-gray-500 active:bg-gray-100"
        >
          ›
        </button>
      )}
    </div>
  );
}
