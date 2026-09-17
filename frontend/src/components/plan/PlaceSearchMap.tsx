"use client";

import { useEffect, useImperativeHandle, useRef, useState, type Ref } from "react";
import { Button } from "@/components/ui/Button";
import { loadKakaoMaps, type KakaoMapInstance, type KakaoMapsApi } from "@/lib/kakaoMaps";
import { searchPlaceAddresses } from "@/lib/api/placeAddresses";
import type { PlaceSearchBounds, PlaceSearchLocation, PlaceSearchResult } from "@/lib/api";

export interface PlaceSearchMapHandle {
  bounds(): PlaceSearchBounds | undefined;
  move(location: PlaceSearchLocation): PlaceSearchBounds | undefined;
}

function boundsOf(map: KakaoMapInstance): PlaceSearchBounds {
  const bounds = map.getBounds();
  return {
    southWestLongitude: bounds.getSouthWest().getLng(),
    southWestLatitude: bounds.getSouthWest().getLat(),
    northEastLongitude: bounds.getNorthEast().getLng(),
    northEastLatitude: bounds.getNorthEast().getLat(),
  };
}

export function PlaceSearchMap({ ref, places, selectedId, onSelect, onMove, onReady }: {
  ref: Ref<PlaceSearchMapHandle>;
  places: PlaceSearchResult[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  onMove: () => void;
  onReady: () => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const map = useRef<KakaoMapInstance | null>(null);
  const sdk = useRef<KakaoMapsApi | null>(null);
  const callbacks = useRef({ onSelect, onMove, onReady });
  useEffect(() => { callbacks.current = { onSelect, onMove, onReady }; }, [onSelect, onMove, onReady]);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [ready, setReady] = useState(false);

  useImperativeHandle(ref, () => ({
    bounds: () => map.current ? boundsOf(map.current) : undefined,
    move: (location) => {
      if (!map.current || !sdk.current) return undefined;
      map.current.setCenter(new sdk.current.LatLng(location.latitude, location.longitude));
      return boundsOf(map.current);
    },
  }), []);

  useEffect(() => {
    let cancelled = false;
    let cleanup = () => {};
    async function initialize() {
      try {
        const maps = await loadKakaoMaps();
        const [address] = await searchPlaceAddresses("경기도 성남시 분당구 판교로 242");
        if (cancelled || !container.current) return;
        if (!address) throw new Error("기본 지도 위치를 찾지 못했습니다.");
        const element = container.current;
        const instance = new maps.Map(element, {
          center: new maps.LatLng(address.latitude, address.longitude),
          level: 4, draggable: true, scrollwheel: false,
        });
        sdk.current = maps;
        map.current = instance;
        const moved = () => callbacks.current.onMove();
        maps.event.addListener(instance, "dragend", moved);
        maps.event.addListener(instance, "zoom_changed", moved);
        const observer = new ResizeObserver(() => instance.relayout());
        observer.observe(element);
        cleanup = () => {
          observer.disconnect();
          maps.event.removeListener(instance, "dragend", moved);
          maps.event.removeListener(instance, "zoom_changed", moved);
          map.current = null;
          element.replaceChildren();
        };
        setReady(true);
        callbacks.current.onReady();
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause.message : "지도를 불러오지 못했습니다.");
      }
    }
    void initialize();
    return () => { cancelled = true; cleanup(); };
  }, [attempt]);

  useEffect(() => {
    if (!ready || !map.current || !sdk.current) return;
    const maps = sdk.current;
    const entries = places.filter((place) => Number.isFinite(place.latitude) && Number.isFinite(place.longitude))
      .map((place) => {
        const selected = place.kakaoId === selectedId;
        const marker = new maps.Marker({
          map: map.current!,
          position: new maps.LatLng(place.latitude!, place.longitude!),
          title: (selected ? "선택: " : "") + place.name,
        });
        if (selected) {
          // 선택 마커의 크기와 테두리를 구분한다.
          const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="48"><path d="M20 46S3 27 3 19a17 17 0 1 1 34 0C37 27 20 46 20 46" fill="#3182F6" stroke="white" stroke-width="3"/><circle cx="20" cy="19" r="6" fill="white"/></svg>';
          marker.setImage(new maps.MarkerImage("data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg), new maps.Size(40, 48)));
        }
        marker.setZIndex(selected ? 10 : 1);
        const click = () => callbacks.current.onSelect(place.kakaoId);
        maps.event.addListener(marker, "click", click);
        return { marker, click };
      });
    return () => entries.forEach(({ marker, click }) => {
      maps.event.removeListener(marker, "click", click);
      marker.setMap(null);
    });
  }, [places, selectedId, ready]);

  return (
    <div className="relative h-full overflow-hidden rounded-2xl border border-gray-200 bg-gray-100">
      <div ref={container} className="absolute inset-0" aria-label="장소 검색 지도" />
      {!ready && <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-gray-100 px-5 text-center text-sm text-gray-700" role="status">
        {error ?? "판교로 242 지도를 불러오는 중…"}
        {error && <Button size="sm" variant="ghost" fullWidth={false} onClick={() => { setError(null); setAttempt((n) => n + 1); }}>다시 시도</Button>}
      </div>}
      {ready && <div className="absolute right-3 top-3 z-10 flex flex-col gap-1">
        <Button size="sm" variant="ghost" aria-label="지도 확대" onClick={() => map.current?.setLevel(Math.max(1, map.current.getLevel() - 1))}>＋</Button>
        <Button size="sm" variant="ghost" aria-label="지도 축소" onClick={() => map.current?.setLevel(Math.min(14, map.current.getLevel() + 1))}>−</Button>
      </div>}
    </div>
  );
}
