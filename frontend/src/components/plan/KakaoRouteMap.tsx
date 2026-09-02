"use client";

import { useEffect, useRef, useState } from "react";
import type { Schedule } from "@/lib/api";

type KakaoLatLng = object;
type KakaoMarkerImage = object;

interface KakaoMapInstance {
  getLevel(): number;
  setLevel(level: number, options?: { animate?: boolean }): void;
  setBounds(bounds: KakaoBounds): void;
}

interface KakaoBounds {
  extend(position: KakaoLatLng): void;
}

interface KakaoMarker {
  setImage(image: KakaoMarkerImage): void;
  setZIndex(zIndex: number): void;
}

interface KakaoMapsApi {
  load(callback: () => void): void;
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
  LatLngBounds: new () => KakaoBounds;
  Size: new (width: number, height: number) => object;
  MarkerImage: new (src: string, size: object) => KakaoMarkerImage;
  Map: new (
    container: HTMLElement,
    options: {
      center: KakaoLatLng;
      level: number;
      draggable: boolean;
      scrollwheel: boolean;
    },
  ) => KakaoMapInstance;
  Marker: new (options: {
    map: KakaoMapInstance;
    position: KakaoLatLng;
    title: string;
    image?: KakaoMarkerImage;
  }) => KakaoMarker;
  Polyline: new (options: {
    map: KakaoMapInstance;
    path: KakaoLatLng[];
    strokeWeight: number;
    strokeColor: string;
    strokeOpacity: number;
    strokeStyle: "solid" | "shortdash" | "shortdot" | "shortdashdot" | "longdash" | "longdot" | "longdashdot" | "dash" | "dot" | "dashdot";
    zIndex: number;
  }) => object;
  event: {
    addListener(target: KakaoMarker, eventName: "click", handler: () => void): void;
  };
  services: {
    Status: { OK: string };
    Geocoder: new () => {
      addressSearch(
        address: string,
        callback: (results: Array<{ x: string; y: string }>, status: string) => void,
      ): void;
    };
    Places: new () => {
      keywordSearch(
        keyword: string,
        callback: (results: Array<{ x: string; y: string }>, status: string) => void,
      ): void;
    };
  };
}

declare global {
  interface Window {
    kakao?: {
      maps?: KakaoMapsApi;
    };
  }
}

function getKakaoMaps(): KakaoMapsApi | undefined {
  return window.kakao?.maps;
}

function loadKakaoMaps(): Promise<KakaoMapsApi> {
  return new Promise((resolve, reject) => {
    const appKey = process.env.NEXT_PUBLIC_KAKAO_JS_KEY;
    if (!appKey) {
      reject(new Error("NEXT_PUBLIC_KAKAO_JS_KEY가 설정되지 않았습니다."));
      return;
    }

    const ready = () => {
      const maps = getKakaoMaps();
      if (!maps) {
        reject(new Error("카카오 지도 SDK를 불러오지 못했습니다."));
        return;
      }
      maps.load(() => resolve(maps));
    };

    if (getKakaoMaps()) {
      ready();
      return;
    }

    const existing = document.querySelector<HTMLScriptElement>("script[data-kakao-maps-sdk]");
    if (existing) {
      existing.addEventListener("load", ready, { once: true });
      existing.addEventListener(
        "error",
        () => {
          existing.remove();
          reject(new Error("카카오 지도 SDK를 불러오지 못했습니다."));
        },
        { once: true },
      );
      return;
    }

    const script = document.createElement("script");
    script.dataset.kakaoMapsSdk = "true";
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;
    script.async = true;
    script.addEventListener("load", ready, { once: true });
    script.addEventListener(
      "error",
      () => {
        script.remove();
        reject(new Error("카카오 지도 SDK를 불러오지 못했습니다."));
      },
      { once: true },
    );
    document.head.appendChild(script);
  });
}

function findPosition(
  maps: KakaoMapsApi,
  schedule: Schedule,
): Promise<KakaoLatLng | null> {
  return new Promise((resolve) => {
    if (!schedule.placeName && !schedule.placeAddress) {
      resolve(null);
      return;
    }

    const useResult = (results: Array<{ x: string; y: string }>, status: string) => {
      if (status !== maps.services.Status.OK || results.length === 0) {
        resolve(null);
        return;
      }
      resolve(new maps.LatLng(Number(results[0].y), Number(results[0].x)));
    };

    if (schedule.placeAddress) {
      new maps.services.Geocoder().addressSearch(schedule.placeAddress, (results, status) => {
        if (status === maps.services.Status.OK && results.length > 0) {
          useResult(results, status);
          return;
        }
        new maps.services.Places().keywordSearch(schedule.placeName, useResult);
      });
      return;
    }

    new maps.services.Places().keywordSearch(schedule.placeName, useResult);
  });
}

function createMarkerImage(
  maps: KakaoMapsApi,
  order: number,
  selected: boolean,
): KakaoMarkerImage {
  const width = selected ? 48 : 36;
  const height = selected ? 58 : 44;
  const fill = selected ? "#1B64DA" : "#3182F6";
  const fontSize = selected ? 17 : 13;
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
      <path fill="${fill}" stroke="#FFFFFF" stroke-width="3" d="M${width / 2} 2C${width * 0.25} 2 4 ${width * 0.23} 4 ${width * 0.48}c0 ${width * 0.31} ${width / 2} ${height - 4} ${width / 2} ${height - 4}s${width / 2}-${height - 4 - width * 0.48} ${width / 2}-${height - 4 - width * 0.48}C${width - 4} ${width * 0.23} ${width * 0.75} 2 ${width / 2} 2Z"/>
      <circle cx="${width / 2}" cy="${width * 0.45}" r="${width * 0.22}" fill="#FFFFFF"/>
      <text x="${width / 2}" y="${width * 0.45 + fontSize * 0.36}" text-anchor="middle" font-family="Arial, sans-serif" font-size="${fontSize}" font-weight="700" fill="${fill}">${order}</text>
    </svg>`;
  const src = `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
  return new maps.MarkerImage(src, new maps.Size(width, height));
}

export function KakaoRouteMap({
  schedules,
  selectedScheduleId,
  onSelectSchedule,
}: {
  schedules: Schedule[];
  selectedScheduleId: string | null;
  onSelectSchedule: (scheduleId: string) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMapInstance | null>(null);
  const mapsRef = useRef<KakaoMapsApi | null>(null);
  const markersRef = useRef<Array<{ marker: KakaoMarker; scheduleId: string; order: number }>>([]);
  const selectedScheduleIdRef = useRef(selectedScheduleId);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function initialize() {
      try {
        const maps = await loadKakaoMaps();
        if (cancelled || !containerRef.current) return;
        const container = containerRef.current;
        container.replaceChildren();

        const map = new maps.Map(container, {
          center: new maps.LatLng(37.5665, 126.978),
          level: 6,
          draggable: true,
          scrollwheel: true,
        });
        mapRef.current = map;
        mapsRef.current = maps;
        markersRef.current = [];

        const positions = await Promise.all(
          schedules.map(async (schedule) => ({
            schedule,
            position: await findPosition(maps, schedule),
          })),
        );
        if (cancelled) return;

        const bounds = new maps.LatLngBounds();
        const routePath: KakaoLatLng[] = [];
        let markerCount = 0;
        positions.forEach(({ schedule, position }, index) => {
          if (!position) return;
          routePath.push(position);
          const marker = new maps.Marker({
            map,
            position,
            title: schedule.placeName,
            image: createMarkerImage(maps, index + 1, selectedScheduleIdRef.current === schedule.id),
          });
          marker.setZIndex(selectedScheduleIdRef.current === schedule.id ? 10 : 1);
          maps.event.addListener(marker, "click", () => onSelectSchedule(schedule.id));
          markersRef.current.push({ marker, scheduleId: schedule.id, order: index + 1 });
          bounds.extend(position);
          markerCount += 1;
        });
        if (routePath.length > 1) {
          new maps.Polyline({
            map,
            path: routePath,
            strokeWeight: 4,
            strokeColor: "#3182F6",
            strokeOpacity: 0.75,
            strokeStyle: "solid",
            zIndex: 0,
          });
        }
        if (markerCount > 0) map.setBounds(bounds);
      } catch (cause) {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : "지도를 불러오지 못했습니다.");
        }
      }
    }

    void initialize();
    return () => {
      cancelled = true;
      mapRef.current = null;
      mapsRef.current = null;
      markersRef.current = [];
    };
  }, [schedules, onSelectSchedule]);

  useEffect(() => {
    selectedScheduleIdRef.current = selectedScheduleId;
    const maps = mapsRef.current;
    if (!maps) return;

    markersRef.current.forEach(({ marker, scheduleId, order }) => {
      const selected = selectedScheduleId === scheduleId;
      marker.setImage(createMarkerImage(maps, order, selected));
      marker.setZIndex(selected ? 10 : 1);
    });
  }, [selectedScheduleId]);

  function changeZoom(delta: number) {
    const map = mapRef.current;
    if (!map) return;
    const nextLevel = Math.min(14, Math.max(1, map.getLevel() + delta));
    map.setLevel(nextLevel, { animate: true });
  }

  return (
    <div className="relative h-full min-h-[300px] overflow-hidden bg-gray-100">
      <div ref={containerRef} className="absolute inset-0" aria-label="일정 장소 지도" />
      {error && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100 px-8 text-center text-[13px] text-gray-700">
          {error}
        </div>
      )}
      <div className="absolute right-3 top-3 z-10 flex flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
        <button
          type="button"
          aria-label="지도 확대"
          onClick={() => changeZoom(-1)}
          className="h-10 w-10 text-xl font-bold text-ink active:bg-gray-100"
        >
          +
        </button>
        <div className="h-px bg-gray-200" />
        <button
          type="button"
          aria-label="지도 축소"
          onClick={() => changeZoom(1)}
          className="h-10 w-10 text-xl font-bold text-ink active:bg-gray-100"
        >
          −
        </button>
      </div>
    </div>
  );
}
