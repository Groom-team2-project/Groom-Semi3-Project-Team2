# Places 도메인 명세

## 1. 목적

Places 도메인은 사용자가 장소를 탐색하고, 선택한 장소를 여행 일정에 바로 등록하거나 일정 후보지에 추가할 수 있도록 지원한다.

백엔드에서 카카오 로컬 API를 호출하여 지도 영역 안의 장소를 검색하고, 프론트엔드가 목록·마커·장소 정보 화면에 사용할 수 있는 형태로 응답한다. 사용자가 장소를 실제 일정 또는 일정 후보지에 추가할 때만 장소를 DB에 저장하며, 단순 검색 결과는 저장하지 않는다.

## 2. 기능 범위

### Places가 담당하는 기능

- 미리 정의된 카테고리를 이용한 장소 검색
- 장소명 또는 일반 키워드를 이용한 장소 검색
- 지도 화면 영역이 바뀌었을 때 해당 영역 재검색
- 카카오 JSON을 서비스 공통 응답 DTO로 변환
- 선택한 장소 등록 및 중복 방지
- 선택한 장소를 일정에 등록하는 흐름 제공
- 선택한 장소를 일정 후보지에 추가하는 흐름 제공
- 저장된 장소 조회

### 다른 영역이 담당하는 기능

- 지도 표시와 마커 렌더링: 프론트엔드
- 지도 이동 종료 감지와 검색 재요청: 프론트엔드
- 일정 후보지의 상태와 플랜 연결 규칙: 후보지 담당 도메인
- 일정의 날짜·시간·순서 및 충돌 검증: Schedules
- 투표 선택지와 장소의 연결: Votes

## 3. 사용자 시나리오

### 3.1 카테고리로 둘러보기

1. 사용자가 화면에 미리 만들어진 카테고리 버튼을 선택한다.
2. 프론트엔드는 선택된 카테고리 코드와 현재 지도 영역을 백엔드에 전달한다.
3. 백엔드는 카카오 카테고리 장소 검색 API를 호출한다.
4. 백엔드는 카카오 JSON에서 필요한 필드만 추출하여 반환한다.
5. 프론트엔드는 같은 응답으로 장소 목록과 지도 마커를 표시한다.
6. 사용자가 지도를 이동하거나 확대·축소한다.
7. 프론트엔드는 지도 이동이 끝난 후 선택된 카테고리와 새로운 지도 영역으로 다시 검색한다.
8. 새로운 장소 목록과 마커로 화면을 갱신한다.

### 3.2 장소 이름으로 검색하기

1. 사용자가 장소명 또는 검색어를 입력한다.
2. 프론트엔드는 검색어와 현재 지도 영역을 백엔드에 전달한다.
3. 백엔드는 카카오 키워드 장소 검색 API를 호출한다.
4. 검색 결과를 공통 장소 응답 형태로 반환한다.
5. 프론트엔드는 결과 목록과 마커를 표시한다.
6. 키워드가 유지된 상태에서 지도를 이동하면 새로운 지도 영역으로 다시 검색한다.

### 3.3 장소 선택 후 일정에 등록

1. 사용자가 목록 또는 지도 마커에서 장소를 선택한다.
2. 프론트엔드는 선택된 장소 정보를 상세 카드에 표시한다.
3. 사용자가 일정에 추가를 선택하고 날짜, 시작·종료 시간, 제목 등의 일정 정보를 입력한다.
4. 백엔드는 플랜 접근 권한과 일정 입력값을 확인한다.
5. Places가 `kakaoPlaceId`를 기준으로 장소를 저장하거나 기존 장소를 조회한다.
6. Schedules가 반환된 `placeId`로 일정을 생성한다.
7. 생성된 일정과 장소 정보를 함께 반환한다.

### 3.4 장소 선택 후 일정 후보지에 추가

1. 사용자가 목록 또는 지도 마커에서 장소를 선택한다.
2. 프론트엔드는 검색 응답에 포함된 장소 정보를 상세 카드에 표시한다.
3. 사용자가 후보지 추가 버튼을 누른다.
4. 백엔드는 플랜 접근 권한을 확인한다.
5. `kakaoPlaceId`가 이미 저장되어 있으면 기존 장소를 사용하고, 없으면 새로 저장한다.
6. 후보지 담당 도메인이 해당 장소를 플랜의 일정 후보지와 연결한다.
7. 생성된 후보지 정보를 반환한다.

## 4. 고정 카테고리 정책

카테고리는 생성·수정·삭제하는 도메인 데이터가 아니다. 프론트엔드에는 선택 버튼을 미리 만들고, 백엔드에는 허용된 코드만 상수 또는 Enum으로 정의한다.

초기 여행 서비스용 권장 카테고리는 다음과 같다.

| 화면 표시명 | 요청 코드 | 카카오 그룹 코드 |
| --- | --- | --- |
| 음식점 | `RESTAURANT` | `FD6` |
| 카페 | `CAFE` | `CE7` |
| 관광명소 | `ATTRACTION` | `AT4` |
| 문화시설 | `CULTURE` | `CT1` |
| 숙박 | `ACCOMMODATION` | `AD5` |
| 주차장 | `PARKING` | `PK6` |

프론트엔드는 카카오 코드 대신 서비스 요청 코드(`RESTAURANT` 등)를 전달한다. 백엔드가 이를 카카오 그룹 코드로 변환하면 외부 API 규격이 프론트엔드 계약에 직접 노출되지 않는다.

카카오 카테고리 검색은 주요 그룹 코드만 지원한다. `한식`, `일식`, `영화관` 같은 세부 버튼도 제공하려면 해당 버튼을 고정 검색 키워드로 정의하고 카카오 키워드 검색 API를 사용한다.

예시:

| 화면 표시명 | 요청 코드 | 검색 방식 | 카카오 요청 값 |
| --- | --- | --- | --- |
| 한식 | `KOREAN_FOOD` | 키워드 | `query=한식`, `category_group_code=FD6` |
| 일식 | `JAPANESE_FOOD` | 키워드 | `query=일식`, `category_group_code=FD6` |
| 영화관 | `MOVIE_THEATER` | 키워드 | `query=영화관`, `category_group_code=CT1` |

DB에 별도의 `place_categories` 테이블을 만들거나 카테고리 CRUD API를 제공하지 않는다.

## 5. 지도 영역 검색 정책

지도 이동 검색에는 카카오 API의 `rect` 파라미터를 사용한다.

```text
rect = southWestLongitude,southWestLatitude,northEastLongitude,northEastLatitude
```

프론트엔드 전달 값:

- `southWestLongitude`: 지도 남서쪽 경도
- `southWestLatitude`: 지도 남서쪽 위도
- `northEastLongitude`: 지도 북동쪽 경도
- `northEastLatitude`: 지도 북동쪽 위도

프론트엔드 동작 규칙:

- 드래그 또는 확대·축소가 끝난 시점에만 요청한다.
- 연속 이벤트는 debounce를 적용한다. 권장 시작값은 300~500ms다.
- 이전 요청이 끝나기 전에 새 검색이 시작되면 이전 요청을 취소하거나 최신 응답만 반영한다.
- 선택된 카테고리 또는 검색어를 유지한 채 지도 영역만 변경한다.
- 동일한 `kakaoPlaceId`는 목록과 마커에서 하나의 장소로 취급한다.

백엔드 동작 규칙:

- 네 좌표가 모두 전달되었는지 검증한다.
- 위도·경도 범위와 남서/북동 좌표 순서를 검증한다.
- 지나치게 넓은 지도 영역 요청은 거부하거나 프론트엔드에 확대를 요청하도록 오류를 반환할 수 있다.
- 검색 결과가 없어도 오류가 아닌 빈 배열을 반환한다.

## 6. 카카오 JSON 매핑

카카오 장소 검색 응답에서 실제로 사용할 필드만 다음과 같이 변환한다.

| 카카오 JSON | 서비스 응답 | 사용 목적 | 저장 여부 |
| --- | --- | --- | --- |
| `id` | `kakaoPlaceId` | 외부 장소 고유 식별 | O |
| `place_name` | `placeName` | 목록, 마커, 상세 카드 | O |
| `category_name` | `categoryName` | 상세 카테고리 표시 | O |
| `category_group_code` | `categoryGroupCode` | 주요 카테고리 구분 | O |
| `category_group_name` | `categoryGroupName` | 주요 카테고리 표시 | O |
| `phone` | `phone` | 상세 카드 | O, 빈 문자열은 NULL |
| `address_name` | `address` | 지번 주소 | O |
| `road_address_name` | `roadAddress` | 도로명 주소 | O, 빈 문자열은 NULL |
| `x` | `longitude` | 지도 마커 경도 | O, 숫자로 변환 |
| `y` | `latitude` | 지도 마커 위도 | O, 숫자로 변환 |
| `place_url` | `placeUrl` | 카카오맵 상세 페이지 이동 | O |
| `distance` | `distanceMeters` | 중심점 검색 시 거리 표시 | X |

카카오 검색 응답에는 대표 이미지가 없으므로 `imageUrl`은 Places 기본 응답과 DB에서 제외한다. 이미지가 필요해지면 별도 출처와 이용 정책을 정한 뒤 확장한다.

목록, 마커, 선택 장소 상세 카드가 같은 `PlaceSearchItem` 응답을 사용한다. 카카오 로컬 API에는 장소 ID로 모든 상세 정보를 다시 조회하는 별도 장소 상세 API가 없으므로, 선택 시 불필요한 재요청을 하지 않는다. 더 자세한 정보는 `placeUrl`을 통해 카카오맵 장소 페이지로 연결한다.

## 7. 데이터 모델

### places

| 필드 | 타입 예시 | 필수 | 제약/설명 |
| --- | --- | --- | --- |
| `place_id` | BIGINT | Y | PK, 내부 식별자 |
| `kakao_place_id` | VARCHAR(50) | Y | UNIQUE |
| `place_name` | VARCHAR(200) | Y | 장소명 |
| `category_name` | VARCHAR(300) | N | 카카오 전체 카테고리 경로 |
| `category_group_code` | VARCHAR(10) | N | 카카오 주요 그룹 코드 |
| `category_group_name` | VARCHAR(50) | N | 카카오 주요 그룹명 |
| `address` | VARCHAR(500) | Y | 지번 주소 |
| `road_address` | VARCHAR(500) | N | 도로명 주소 |
| `latitude` | DECIMAL(10,7) | Y | 위도, -90~90 |
| `longitude` | DECIMAL(10,7) | Y | 경도, -180~180 |
| `phone` | VARCHAR(30) | N | 전화번호 |
| `place_url` | VARCHAR(2048) | N | 카카오맵 장소 페이지 URL |
| `created_at` | TIMESTAMP | Y | 생성 일시 |
| `updated_at` | TIMESTAMP | Y | 수정 일시 |

필수 제약:

```sql
UNIQUE (kakao_place_id)
```

## 8. API 명세

### 8.1 장소 검색

카테고리 검색과 장소명 검색을 하나의 API로 제공한다.

`GET /api/places/search`

#### Query parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `category` | string | 조건부 | 고정 카테고리 요청 코드 |
| `query` | string | 조건부 | 장소명 또는 검색어, 공백 제거 후 1~100자 |
| `southWestLongitude` | number | Y | 지도 남서쪽 경도 |
| `southWestLatitude` | number | Y | 지도 남서쪽 위도 |
| `northEastLongitude` | number | Y | 지도 북동쪽 경도 |
| `northEastLatitude` | number | Y | 지도 북동쪽 위도 |
| `page` | integer | N | 1~45, 기본값 1 |
| `size` | integer | N | 1~15, 기본값 15 |

검색 규칙:

- `query`가 있으면 카카오 키워드 검색 API를 사용한다.
- `query`가 없고 `category`가 있으면 카테고리 설정에 따라 카카오 카테고리 또는 키워드 검색 API를 사용한다.
- `query`와 `category`가 모두 없으면 `400 INVALID_PLACE_SEARCH_CONDITION`을 반환한다.
- `query`와 `category`가 함께 있으면 선택 카테고리 안에서 장소명을 검색할 수 있도록 카카오 키워드 검색의 `query`와 `category_group_code`를 함께 사용한다.
- 지원하지 않는 카테고리 요청 코드는 거부한다.

#### Response: `200 OK`

```json
{
  "places": [
    {
      "kakaoPlaceId": "123456789",
      "placeName": "예시식당 광화문점",
      "categoryName": "음식점 > 한식 > 육류,고기",
      "categoryGroupCode": "FD6",
      "categoryGroupName": "음식점",
      "phone": "02-000-0000",
      "address": "서울 종로구 세종로 1",
      "roadAddress": "서울 종로구 세종대로 1",
      "longitude": 126.976,
      "latitude": 37.572,
      "placeUrl": "https://place.map.kakao.com/123456789",
      "distanceMeters": null
    }
  ],
  "page": 1,
  "size": 15,
  "totalCount": 35,
  "hasNext": true
}
```

`longitude`와 `latitude`는 프론트엔드가 그대로 마커 좌표로 사용할 수 있다. `distanceMeters`는 `rect` 검색에서는 제공되지 않을 수 있으므로 nullable이다.

### 8.2 선택한 장소를 일정에 등록

`POST /api/plans/{planId}/schedules`

#### Request

```json
{
  "place": {
    "kakaoPlaceId": "123456789",
    "placeName": "예시식당 광화문점",
    "categoryName": "음식점 > 한식 > 육류,고기",
    "categoryGroupCode": "FD6",
    "categoryGroupName": "음식점",
    "phone": "02-000-0000",
    "address": "서울 종로구 세종로 1",
    "roadAddress": "서울 종로구 세종대로 1",
    "longitude": 126.976,
    "latitude": 37.572,
    "placeUrl": "https://place.map.kakao.com/123456789"
  },
  "scheduleDate": "2026-08-15",
  "title": "점심 식사",
  "startTime": "12:00",
  "endTime": "13:30",
  "memo": "예약 확인"
}
```

#### 처리

1. 플랜 멤버십과 일정 편집 권한을 확인한다.
2. Places가 장소를 조회하거나 저장한다.
3. Schedules가 날짜·시간·일정 충돌 등 일정 규칙을 검증한다.
4. 반환된 `placeId`로 일정을 생성한다.

#### Response: `201 Created`

```json
{
  "scheduleId": 301,
  "planId": 7,
  "scheduleDate": "2026-08-15",
  "title": "점심 식사",
  "startTime": "12:00",
  "endTime": "13:30",
  "place": {
    "placeId": 42,
    "kakaoPlaceId": "123456789",
    "placeName": "예시식당 광화문점",
    "latitude": 37.572,
    "longitude": 126.976
  }
}
```

### 8.3 선택한 장소를 일정 후보지에 추가

`POST /api/plans/{planId}/candidate-places`

#### Request

```json
{
  "place": {
    "kakaoPlaceId": "123456789",
    "placeName": "예시식당 광화문점",
    "categoryName": "음식점 > 한식 > 육류,고기",
    "categoryGroupCode": "FD6",
    "categoryGroupName": "음식점",
    "phone": "02-000-0000",
    "address": "서울 종로구 세종로 1",
    "roadAddress": "서울 종로구 세종대로 1",
    "longitude": 126.976,
    "latitude": 37.572,
    "placeUrl": "https://place.map.kakao.com/123456789"
  }
}
```

#### 처리

1. 사용자가 해당 플랜의 Editor 또는 Owner인지 확인한다.
2. 요청 장소의 필수 값과 좌표를 검증한다.
3. `kakaoPlaceId`로 장소를 조회한다.
4. 없으면 `places`에 저장하고, 있으면 기존 행을 사용한다.
5. 후보지와 `placeId`를 연결한다.
6. 같은 플랜에 같은 장소가 이미 있으면 중복 오류를 반환한다.

#### Response: `201 Created`

```json
{
  "candidatePlaceId": 91,
  "planId": 7,
  "placeId": 42,
  "kakaoPlaceId": "123456789",
  "placeName": "예시식당 광화문점"
}
```

현재 ERD처럼 후보지를 `vote_options`로만 관리한다면 `candidatePlaceId` 대신 `optionId`를 반환하도록 Votes API와 통합한다.

### 8.4 저장된 장소 조회

`GET /api/places/{placeId}`

후보지 또는 일정에 이미 저장된 장소를 조회할 때 사용한다.

## 9. 도메인 규칙

1. 검색 결과는 DB에 저장하지 않는다.
2. 사용자가 일정 등록 또는 일정 후보지 추가를 확정할 때만 장소를 저장한다.
3. `kakaoPlaceId`가 같은 장소는 하나만 저장한다.
4. 동시 저장 요청의 중복은 DB UNIQUE 제약으로 최종 방지한다.
5. 같은 장소를 여러 플랜에서 재사용할 수 있다.
6. 참조 중인 장소는 물리 삭제하지 않는다.
7. 카카오가 빈 문자열로 반환한 선택 정보는 API 응답에서 `null`로 변환한다.
8. 카카오의 문자열 좌표는 숫자로 변환하고 범위를 검증한다.
9. 외부 API 오류 메시지와 REST API 키를 클라이언트에 노출하지 않는다.
10. 클라이언트가 보낸 카테고리 코드를 카카오 API에 그대로 전달하지 않고 허용 목록으로 검증한다.
11. 일정 또는 후보지 등록이 실패하면 새 장소만 단독으로 남지 않도록 장소 저장과 연결 생성을 하나의 트랜잭션으로 처리한다.

## 10. 오류 명세

| HTTP 상태 | 오류 코드 | 발생 조건 |
| --- | --- | --- |
| 400 | `INVALID_PLACE_SEARCH_CONDITION` | 카테고리와 검색어가 모두 없거나 검색 조건이 잘못됨 |
| 400 | `INVALID_MAP_BOUNDS` | 지도 영역 좌표가 누락되거나 유효하지 않음 |
| 400 | `UNSUPPORTED_PLACE_CATEGORY` | 허용되지 않은 카테고리 코드 |
| 400 | `INVALID_PLACE_DATA` | 일정 또는 후보지로 저장할 장소 정보가 유효하지 않음 |
| 401 | `UNAUTHORIZED` | 인증이 필요함 |
| 403 | `PLAN_EDIT_FORBIDDEN` | 해당 플랜에 일정 또는 후보지를 추가할 권한이 없음 |
| 404 | `PLACE_NOT_FOUND` | 저장된 장소를 찾을 수 없음 |
| 409 | `CANDIDATE_PLACE_ALREADY_EXISTS` | 같은 플랜에 이미 등록된 후보지 |
| 429 | `PLACE_API_RATE_LIMITED` | 카카오 API 호출 한도 초과 |
| 502 | `PLACE_PROVIDER_ERROR` | 카카오 API 오류 또는 잘못된 응답 |
| 504 | `PLACE_PROVIDER_TIMEOUT` | 카카오 API 응답 시간 초과 |

## 11. 테스트 시나리오

### 검색

- 카테고리와 지도 영역을 전달하면 해당 영역의 장소가 반환된다.
- 장소명과 지도 영역을 전달하면 키워드 검색 결과가 반환된다.
- 카테고리와 장소명을 함께 전달하면 해당 그룹 안에서 검색된다.
- 지도 영역을 옮기면 새 `rect` 값으로 검색된다.
- 검색 결과가 없으면 빈 `places` 배열이 반환된다.
- 허용되지 않은 카테고리는 카카오 API 호출 전에 거부된다.
- 잘못된 좌표와 뒤집힌 지도 경계는 거부된다.

### 응답 변환

- 카카오의 `id`, 장소명, 분류, 주소, 좌표, 전화번호, URL만 응답에 포함한다.
- `x`는 `longitude`, `y`는 `latitude`로 정확히 변환한다.
- 빈 전화번호와 도로명 주소는 `null`로 변환한다.
- 카카오 API 키와 원본 오류 본문은 응답에 포함하지 않는다.

### 후보지 추가

- 장소 선택만으로는 DB에 행이 생성되지 않는다.
- 최초 후보지 추가 시 장소와 후보지 연결이 생성된다.
- 같은 카카오 장소를 다시 추가하면 기존 `places` 행을 사용한다.
- 같은 플랜에 같은 후보지를 중복 추가할 수 없다.
- Viewer는 후보지를 추가할 수 없다.

### 일정 등록

- 검색 결과의 장소와 일정 정보를 전달하면 장소와 일정이 함께 생성된다.
- 이미 저장된 장소라면 기존 `places` 행을 일정에 연결한다.
- 일정 입력값이 잘못되면 일정과 장소 연결을 생성하지 않는다.
- Viewer는 일정을 등록할 수 없다.

### 지도 연동 인수 조건

- 검색 결과 개수와 표시된 마커 개수가 일치한다.
- 목록의 장소를 선택하면 동일 `kakaoPlaceId`의 마커가 강조된다.
- 마커를 선택하면 동일 `kakaoPlaceId`의 목록 항목과 상세 카드가 표시된다.
- 지도 이동 후에는 기존 카테고리 또는 검색어가 유지된 결과가 표시된다.
- 늦게 도착한 이전 검색 응답이 최신 지도 결과를 덮어쓰지 않는다.

## 12. 구현 전 확정 사항

1. 초기 고정 카테고리 버튼 목록
2. 세부 카테고리 버튼을 제공할지 여부
3. 검색 API를 로그인 사용자에게만 허용할지 여부
4. 지도 한 화면에서 표시할 최대 마커 수
5. 후보지를 별도 `plan_places`로 관리할지 `vote_options`로 바로 관리할지 여부
6. 후보지 추가 시 클라이언트가 전달한 장소 데이터를 그대로 검증·저장할지, 짧은 서버 캐시나 서명 토큰을 사용할지 여부

## 13. ERD 검토 메모

현재 ERD의 `places`는 `schedules`와 `vote_options`에서 공동으로 참조하는 장소 원본 테이블로 사용할 수 있다.

투표 생성 전에도 플랜에 후보 장소를 저장하려면 아래 연결 테이블이 추가로 필요하다.

```text
plan_places
- plan_place_id (PK)
- plan_id (FK)
- place_id (FK)
- created_by (FK, user_id)
- created_at
- UNIQUE(plan_id, place_id)
```

모든 후보 장소가 생성 즉시 투표 선택지가 되는 흐름이면 별도 `plan_places` 없이 현재 `vote_options` 구조를 사용할 수 있다.
