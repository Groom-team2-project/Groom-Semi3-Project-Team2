import { loadKakaoMaps } from "@/lib/kakaoMaps";

export interface PlaceAddress {
  address: string;
  latitude: number;
  longitude: number;
}

export async function searchPlaceAddresses(query: string): Promise<PlaceAddress[]> {
  const maps = await loadKakaoMaps();
  return new Promise((resolve, reject) => {
    const timeout = window.setTimeout(() => reject(new Error("주소 검색 시간이 초과되었습니다.")), 8000);
    new maps.services.Geocoder().addressSearch(query, (results, status) => {
      window.clearTimeout(timeout);
      if (status === maps.services.Status.ZERO_RESULT) { resolve([]); return; }
      if (status !== maps.services.Status.OK) { reject(new Error("주소를 검색하지 못했습니다.")); return; }
      resolve(results.map((item) => ({
        address: item.address_name, latitude: Number(item.y), longitude: Number(item.x),
      })).filter((item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude)));
    });
  });
}
