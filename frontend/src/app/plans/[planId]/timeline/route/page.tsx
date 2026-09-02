"use client";

import { use, useCallback, useEffect, useState } from "react";
import { AppBar } from "@/components/ui/AppBar";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { KakaoRouteMap } from "@/components/plan/KakaoRouteMap";
import { getSchedules } from "@/lib/api";
import type { Schedule } from "@/lib/api";

export default function RouteMapPage({
  params,
  searchParams,
}: {
  params: Promise<{ planId: string }>;
  searchParams: Promise<{ day?: string }>;
}) {
  const { planId } = use(params);
  const { day: dayParam } = use(searchParams);
  const day = Number(dayParam) || 1;
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [selectedScheduleId, setSelectedScheduleId] = useState<string | null>(null);

  useEffect(() => {
    getSchedules(planId).then((all) =>
      setSchedules(all.filter((s) => s.day === day)),
    );
  }, [planId, day]);

  const locatedSchedules = schedules.filter((schedule) =>
    Boolean(schedule.placeId || schedule.placeAddress),
  );
  const selectedSchedule = locatedSchedules.find((schedule) => schedule.id === selectedScheduleId) ?? null;

  const toggleSchedule = useCallback((scheduleId: string) => {
    setSelectedScheduleId((current) => current === scheduleId ? null : scheduleId);
  }, []);

  return (
    <div className="flex min-h-dvh flex-col">
      <AppBar
        title={`Day ${day} 장소 지도`}
        subtitle={locatedSchedules.length > 0 ? `일정 장소 ${locatedSchedules.length}곳` : "표시할 장소 없음"}
        backHref={`/plans/${planId}/timeline?day=${day}`}
      />

      {locatedSchedules.length === 0 ? (
        <div className="px-4">
          <EmptyState
            emoji="🗺️"
            title="지도에 표시할 장소가 없어요"
            description="일정에 장소를 선택하면 지도에 마커로 표시돼요"
          />
        </div>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col bg-gray-100">
          <div className="relative h-[46dvh] min-h-[300px] shrink-0 border-b border-gray-200">
            <KakaoRouteMap
              schedules={locatedSchedules}
              selectedScheduleId={selectedScheduleId}
              onSelectSchedule={toggleSchedule}
            />
            <div className="absolute left-2.5 top-2.5 rounded-lg border border-gray-200 bg-white px-2.5 py-1 text-[10.5px] font-bold text-gray-700">
              숫자 순서대로 연결된 동선 · 마커를 누르면 상세 보기
            </div>
          </div>

          <section className="flex min-h-0 flex-1 flex-col bg-white" aria-label="일정 목록 및 상세">
            <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
              <div>
                <h2 className="text-[15px] font-bold text-ink">
                  {selectedSchedule ? "선택한 일정" : `Day ${day} 일정`}
                </h2>
                <p className="mt-0.5 text-[11.5px] text-gray-500">
                  {selectedSchedule ? "마커를 다시 누르거나 아래 버튼으로 목록에 돌아갈 수 있어요" : "시간 순서대로 둘러보세요"}
                </p>
              </div>
              <span className="rounded-full bg-primary-soft px-2.5 py-1 text-[11px] font-bold text-primary-dark">
                {locatedSchedules.length}곳
              </span>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-3">
              {selectedSchedule ? (
                <Card className="gap-3 border-primary bg-primary-soft">
                  <div className="flex items-start gap-3">
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-[13px] font-bold text-white">
                      {locatedSchedules.findIndex((schedule) => schedule.id === selectedSchedule.id) + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="truncate text-[15px] font-bold text-ink">
                          {selectedSchedule.emoji} {selectedSchedule.title ?? selectedSchedule.placeName}
                        </h3>
                        <time className="shrink-0 font-mono text-[12px] font-bold text-primary">
                          {selectedSchedule.time}{selectedSchedule.endAt ? ` ~ ${selectedSchedule.endAt.slice(11, 16)}` : ""}
                        </time>
                      </div>
                      <dl className="mt-3 grid grid-cols-[58px_1fr] gap-x-2 gap-y-1.5 border-t border-gray-200 pt-3 text-[12px] leading-relaxed">
                        <dt className="font-bold text-gray-500">장소</dt>
                        <dd className="font-semibold text-ink">{selectedSchedule.placeName}</dd>
                        <dt className="font-bold text-gray-500">주소</dt>
                        <dd className="text-gray-700">{selectedSchedule.placeAddress || "주소 정보 없음"}</dd>
                        <dt className="font-bold text-gray-500">전화</dt>
                        <dd className="min-w-0 text-gray-700">{selectedSchedule.placePhone || "전화번호 없음"}</dd>
                        <dt className="font-bold text-gray-500">장소 URL</dt>
                        <dd className="min-w-0 break-all">
                          {selectedSchedule.placeUrl ? (
                            <a
                              href={selectedSchedule.placeUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="font-semibold text-primary-dark underline underline-offset-2"
                            >
                              {selectedSchedule.placeUrl}
                            </a>
                          ) : (
                            <span className="text-gray-700">장소 URL 없음</span>
                          )}
                        </dd>
                      </dl>
                    </div>
                  </div>
                  {selectedSchedule.memo && (
                    <p className="rounded-xl bg-white px-3.5 py-3 text-[13px] leading-relaxed text-gray-700">
                      {selectedSchedule.memo}
                    </p>
                  )}
                  <button
                    type="button"
                    onClick={() => toggleSchedule(selectedSchedule.id)}
                    className="w-full rounded-xl bg-white py-2.5 text-center text-[12px] font-bold text-primary-dark active:opacity-80"
                  >
                    전체 일정 보기
                  </button>
                </Card>
              ) : (
                <div className="flex flex-col gap-2">
                  {locatedSchedules.map((schedule, index) => (
                    <Card key={schedule.id} onClick={() => toggleSchedule(schedule.id)} className="flex-row items-center gap-3 py-3">
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-[13px] font-bold text-primary-dark">
                        {index + 1}
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <h3 className="truncate font-bold text-ink">
                            {schedule.emoji} {schedule.title ?? schedule.placeName}
                          </h3>
                          <time className="shrink-0 font-mono text-[12px] font-bold text-primary">{schedule.time}</time>
                        </div>
                        <p className="truncate text-[12px] text-gray-500">{schedule.placeAddress || schedule.placeName}</p>
                      </div>
                      <span aria-hidden="true" className="text-gray-300">›</span>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
