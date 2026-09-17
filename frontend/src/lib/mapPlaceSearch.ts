export interface PlaceSearchBounds {
  southWestLongitude: number;
  southWestLatitude: number;
  northEastLongitude: number;
  northEastLatitude: number;
}

export interface MapPlaceQuery {
  keyword?: string;
  category?: string;
  bounds?: PlaceSearchBounds;
}


export function buildMapSearchPath(query: MapPlaceQuery, page = 1): string | null {
  const keyword = query.keyword?.trim();
  if (!keyword && !query.category) return null;
  if (!keyword && !query.bounds) throw new Error("카테고리 검색에는 지도 영역이 필요합니다.");
  const params = new URLSearchParams({ page: String(page), size: "15" });
  if (keyword) params.set("keyword", keyword);
  if (keyword && query.category) params.set("categoryGroupCode", query.category);
  if (query.bounds) Object.entries(query.bounds).forEach(([key, value]) => params.set(key, String(value)));
  const path = keyword ? "/api/v1/place/search" : `/api/v1/place/category/${encodeURIComponent(query.category!)}`;
  return `${path}?${params}`;
}
