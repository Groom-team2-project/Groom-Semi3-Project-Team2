"use client";

import { useEffect, useRef, useState } from "react";
import type { Schedule } from "@/lib/api";

type KakaoLatLng = object;

interface KakaoMapInstance {
  getLevel(): number;
  setLevel(level: number, options?: { animate?: boolean }): void;
  setBounds(bounds: KakaoBounds): void;
}

interface KakaoBounds {
  extend(position: KakaoLatLng): void;
}

interface KakaoMapsApi {
  load(callback: () => void): void;
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
  LatLngBounds: new () => KakaoBounds;
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
  }) => object;
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

function getKakaoMaps(): KakaoMapsApi | undefined {
  const kakao = window.Kakao as (typeof window.Kakao & { maps?: KakaoMapsApi }) | undefined;
  return kakao?.maps;
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
      existing.addEventListener("error", () => reject(new Error("카카오 지도 SDK를 불러오지 못했습니다.")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.dataset.kakaoMapsSdk = "true";
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;
    script.async = true;
    script.addEventListener("load", ready, { once: true });
    script.addEventListener("error", () => reject(new Error("카카오 지도 SDK를 불러오지 못했습니다.")), { once: true });
    document.head.appendChild(script);
  });
}

function findPosition(
  maps: KakaoMapsApi,
  schedule: Schedule,
): Promise<KakaoLatLng | null> {
  return new Promise((resolve) => {
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

export function KakaoRouteMap({ schedules }: { schedules: Schedule[] }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMapInstance | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function initialize() {
      try {
        const maps = await loadKakaoMaps();
        if (cancelled || !containerRef.current) return;

        const map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(37.5665, 126.978),
          level: 6,
          draggable: true,
          scrollwheel: true,
        });
        mapRef.current = map;

        const positions = await Promise.all(
          schedules.map(async (schedule) => ({
            schedule,
            position: await findPosition(maps, schedule),
          })),
        );
        if (cancelled) return;

        const bounds = new maps.LatLngBounds();
        let markerCount = 0;
        positions.forEach(({ schedule, position }) => {
          if (!position) return;
          new maps.Marker({ map, position, title: schedule.placeName });
          bounds.extend(position);
          markerCount += 1;
        });
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
    };
  }, [schedules]);

  function changeZoom(delta: number) {
    const map = mapRef.current;
    if (!map) return;
    const nextLevel = Math.min(14, Math.max(1, map.getLevel() + delta));
    map.setLevel(nextLevel, { animate: true });
  }

  return (
    <div className="relative h-[260px] overflow-hidden bg-gray-100">
      <div ref={containerRef} className="h-full w-full" aria-label="일정 경로 지도" />
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
