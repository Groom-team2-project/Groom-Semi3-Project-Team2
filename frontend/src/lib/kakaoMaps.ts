export interface KakaoLatLng { getLat(): number; getLng(): number; }
export type KakaoMarkerImage = object;

export interface KakaoMapInstance {
  getBounds(): KakaoBounds;
  setCenter(position: KakaoLatLng): void;
  relayout(): void;
  getLevel(): number;
  setLevel(level: number, options?: { animate?: boolean }): void;
  setBounds(bounds: KakaoBounds): void;
}

export interface KakaoBounds {
  getSouthWest(): KakaoLatLng;
  getNorthEast(): KakaoLatLng;
  extend(position: KakaoLatLng): void;
}

export interface KakaoMarker {
  setMap(map: KakaoMapInstance | null): void;
  setImage(image: KakaoMarkerImage): void;
  setZIndex(zIndex: number): void;
}

export interface KakaoMapsApi {
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
    addListener(target: KakaoMarker | KakaoMapInstance, eventName: string, handler: () => void): void;
    removeListener(target: KakaoMarker | KakaoMapInstance, eventName: string, handler: () => void): void;
  };
  services: {
    Status: { OK: string; ZERO_RESULT: string };
    Geocoder: new () => {
      addressSearch(
        address: string,
        callback: (results: Array<{ x: string; y: string; address_name: string }>, status: string) => void,
      ): void;
    };
    Places: new () => {
      keywordSearch(
        keyword: string,
        callback: (results: Array<{ x: string; y: string; address_name: string }>, status: string) => void,
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

let sdkPromise: Promise<KakaoMapsApi> | null = null;

export function loadKakaoMaps(): Promise<KakaoMapsApi> {
  if (sdkPromise) return sdkPromise;
  sdkPromise = new Promise<KakaoMapsApi>((resolve, reject) => {
    const appKey = process.env.NEXT_PUBLIC_KAKAO_JS_KEY;
    if (!appKey) {
      reject(new Error("지도를 불러올 수 없습니다. 지도 설정을 확인해 주세요."));
      return;
    }
    let script = document.querySelector<HTMLScriptElement>("script[data-kakao-maps-sdk]");
    const timeout = window.setTimeout(() => fail(), 15000);
    const cleanup = () => {
      window.clearTimeout(timeout);
      script?.removeEventListener("load", ready);
      script?.removeEventListener("error", fail);
    };
    const fail = () => {
      cleanup();
      script?.remove();
      reject(new Error("지도 연결에 실패했습니다. 다시 시도해 주세요."));
    };
    const ready = () => {
      const maps = getKakaoMaps();
      if (!maps) { fail(); return; }
      maps.load(() => { cleanup(); resolve(maps); });
    };
    if (getKakaoMaps()) { ready(); return; }
    if (!script) {
      script = document.createElement("script");
      script.dataset.kakaoMapsSdk = "true";
      script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false&libraries=services`;
      script.async = true;
      script.addEventListener("load", ready, { once: true });
      script.addEventListener("error", fail, { once: true });
      document.head.appendChild(script);
    } else {
      script.addEventListener("load", ready, { once: true });
      script.addEventListener("error", fail, { once: true });
    }
  }).catch((error) => { sdkPromise = null; throw error; });
  return sdkPromise;
}
