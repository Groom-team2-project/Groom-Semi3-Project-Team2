export interface RoutePosition {
  latitude: number;
  longitude: number;
}

/** 지도 마커 사이의 직선거리(m). 도보·자동차 경로 거리가 아니다. */
export function straightLineDistance(from: RoutePosition, to: RoutePosition): number {
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const a = Math.sin(radians(to.latitude - from.latitude) / 2) ** 2
    + Math.cos(radians(from.latitude)) * Math.cos(radians(to.latitude))
    * Math.sin(radians(to.longitude - from.longitude) / 2) ** 2;
  return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
}

export function formatRouteDistance(meters: number): string {
  const rounded = Math.round(meters);
  return rounded < 1000 ? `${rounded} m` : `${(meters / 1000).toFixed(1)} km`;
}
