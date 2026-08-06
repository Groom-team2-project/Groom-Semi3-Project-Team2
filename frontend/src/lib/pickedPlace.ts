import type { Place } from "@/lib/api";

const KEY = "tripmate_picked_place";

/** 장소 검색 화면에서 "+"를 누르면 선택한 장소를 여기에 저장하고 원래 화면으로 돌아갑니다. */
export function setPickedPlace(place: Place) {
  try {
    window.sessionStorage.setItem(KEY, JSON.stringify(place));
  } catch {
    // ignore
  }
}

/** 돌아온 화면에서 한 번만 소비(consume) — 읽고 나면 지웁니다. */
export function consumePickedPlace(): Place | null {
  try {
    const raw = window.sessionStorage.getItem(KEY);
    if (!raw) return null;
    window.sessionStorage.removeItem(KEY);
    return JSON.parse(raw) as Place;
  } catch {
    return null;
  }
}
