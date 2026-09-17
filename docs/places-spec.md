# Place

## 구현 결정 사항

- 새 명세의 필드 구성을 기준으로 구현하며 기존 V1 마이그레이션은 수정하지 않는다.
- 모든 API 응답은 프로젝트 공통 형식인 `CommonResponse<T>`로 감싼다. 아래 API별 JSON 예시는 일부가 `data` 중심으로 축약되어 있다.
- Place는 Plan에 종속되지 않는 공용 장소 데이터다. Schedule과 Vote에서는 내부 `placeId`로 참조한다.
- 특정 Plan에 저장한 장소 목록은 `plan_places`로 관리한다. API는 `docs/plan-spec.md`의 PlanPlace를 따른다.
- 사용자는 Place를 직접 생성·수정·삭제하지 않는다. 카카오 Local 검색 과정에서 저장 또는 갱신한다.
- Schedule에 Place를 연결하는 권한은 Schedule 서비스에서 Plan 편집 권한으로 검증한다.

### 공통 응답 형식

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "장소 조회 성공"
}
```

## Entity

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| place_id | BIGINT | PK, AUTO_INCREMENT |  |
| kakao_place_id | VARCHAR(50) | NOT NULL, UNIQUE | 카카오 로컬 API 원본 ID, 중복 저장 방지 |
| name | VARCHAR(200) | NOT NULL | 장소명 |
| category | VARCHAR(255) | NULL |  |
| address | VARCHAR(300) | NULL | 지번 주소 |
| road_address | VARCHAR(300) | NULL | 도로명 주소 |
| latitude | DECIMAL(10,7) | NULL | 위도(y) |
| longitude | DECIMAL(10,7) | NULL | 경도(x) |
| phone | VARCHAR(30) | NULL |  |
| place_url | VARCHAR(500) | NULL |  |
| created_at | DATETIME(6) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(6)  |  |
| updated_at | DATETIME(6) | NOT NULL, DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) |  |
| deleted_at | DATETIME(6) | NULL |  |

## API 명세

> 
> 
> - 카카오 Local API를 통해 장소를 검색하고, 검색 결과 중 서비스에 필요한 정보만 내부 데이터베이스에 저장
>     
>     사용자는 장소를 직접 생성·수정·삭제할 수 없으며, 공개 API에서는 장소 검색과 조회만 가능하다.
>     
>     장소 검색 시 서버는 카카오 Local API를 호출하고, `kakaoPlaceId`를 기준으로 다음 작업을 수행한다.
>     
>     - 저장되지 않은 장소: 신규 저장
>     - 이미 저장된 장소: 카카오 최신 정보로 갱신
>     - 동일한 `kakaoPlaceId`를 가진 장소: 중복 저장하지 않음
>     - 소프트 삭제된 장소: 사용자 응답에서 제외
> - 장소 데이터
>     
>     ### 3.1 내부 저장 데이터
>     
>     | 필드 | 타입 | NULL | 설명 |
>     | --- | --- | --- | --- |
>     | `placeId` | BIGINT | X | 서비스 내부 장소 ID |
>     | `kakaoPlaceId` | VARCHAR(50) | X | 카카오 Local API 장소 ID |
>     | `name` | VARCHAR(200) | X | 장소명 |
>     | `category` | VARCHAR(255) | O | 카카오 카테고리 |
>     | `address` | VARCHAR(300) | O | 지번 주소 |
>     | `roadAddress` | VARCHAR(300) | O | 도로명 주소 |
>     | `latitude` | DECIMAL(10,7) | O | 위도, 카카오 응답의 `y` |
>     | `longitude` | DECIMAL(10,7) | O | 경도, 카카오 응답의 `x` |
>     | `phone` | VARCHAR(30) | O | 전화번호 |
>     | `placeUrl` | VARCHAR(500) | O | 카카오맵 장소 URL |
>     | `createdAt` | DATETIME(6) | X | 최초 저장 시각 |
>     | `updatedAt` | DATETIME(6) | X | 마지막 갱신 시각 |
>     | `deletedAt` | DATETIME(6) | O | 소프트 삭제 시각 |
>     
>     ### 3.2 카카오 응답 매핑
>     
>     | 카카오 응답 필드 | Place 필드 | 변환 규칙 |
>     | --- | --- | --- |
>     | `id` | `kakaoPlaceId` | 문자열로 저장 |
>     | `place_name` | `name` | 그대로 저장 |
>     | `category_name` | `category` | 빈 문자열이면 `NULL` |
>     | `address_name` | `address` | 빈 문자열이면 `NULL` |
>     | `road_address_name` | `roadAddress` | 빈 문자열이면 `NULL` |
>     | `y` | `latitude` | `BigDecimal`로 변환 |
>     | `x` | `longitude` | `BigDecimal`로 변환 |
>     | `phone` | `phone` | 빈 문자열이면 `NULL` |
>     | `place_url` | `placeUrl` | 빈 문자열이면 `NULL` |

### GET /api/v1/places/search

카카오 Local API를 사용해 장소를 검색(카테고리/ 키워드 이용)

#### Request 파라미터

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `query` | String | O | 없음 | 검색 키워드 |
| `page` | Integer | X | `1` | 카카오 검색 페이지 |
| `size` | Integer | X | `15` | 페이지당 결과 수, 최대 15 |
| `sort` | String | X | `accuracy` | `accuracy` 또는 `distance` |
| `latitude` | Decimal | 조건부 | 없음 | 현재 위치의 위도 |
| `longitude` | Decimal | 조건부 | 없음 | 현재 위치의 경도 |
| `radius` | Integer | X | 없음 | 검색 반경, 단위는 미터 |

```json
GET /api/v1/places/search?query=성수동%20카페&page=1&size=15
GET /api/v1/places/search?query=카페&latitude=37.5446891&longitude=127.0583769&radius=2000&sort=distance
```

#### Response Body

- errorcode
    
    
    | 상태 | 오류 코드 | 조건 |
    | --- | --- | --- |
    | 400 | `PLACE_QUERY_REQUIRED` | 검색어 누락 |
    | 400 | `INVALID_PLACE_PAGE_SIZE` | 잘못된 페이지 크기 |
    | 400 | `INVALID_LOCATION` | 잘못된 위치 정보 |
    | 502 | `KAKAO_LOCAL_API_ERROR` | 카카오 API 호출 실패 |

```json
200 OK(장소 있을 때)
{
  "data": {
    "places": [
      {
        "placeId": 1,
        "name": "어니언 성수",
        "address": "서울 성동구 성수동2가 277-135",
        "roadAddress": "서울 성동구 아차산로9길 8",
        "phone": "02-1644-1941",
        "placeUrl": "https://place.map.kakao.com/123456789"
      }
    ],
    "page": 1,
    "size": 15,
    "totalCount": 120,
    "hasNext": true
  }
}
```

```json
200 OK(장소 없을 때)
{
  "data": {
    "places": [],
    "page": 1,
    "size": 15,
    "totalCount": 0,
    "hasNext": false
  }
}
```

### GET /api/v1/places/{placeId}

DB에 저장된 장소를 서비스 내부 placeId로 조회

#### Response Body

- errorcode
    
    
    | 상태 | 오류 코드 | 조건 |
    | --- | --- | --- |
    | 404 | `PLACE_NOT_FOUND` | 장소 없음 |

```json
200 OK
{
  "data": {
    "placeId": 1,
    "name": "어니언 성수",
    "address": "서울 성동구 성수동2가 277-135",
    "roadAddress": "서울 성동구 아차산로9길 8",
    "phone": "02-1644-1941",
    "placeUrl": "https://place.map.kakao.com/123456789"
  }
}
```

### GET /api/v1/places

DB에 저장된 장소 목록 제공(지도에 나와있는 저장된 모든 장소)

#### Request 파라미터

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | Integer | X | `0` | 페이지 번호 |
| `size` | Integer | X | `20` | 페이지 크기, 최대 100 |
| `keyword` | String | X | 없음 | 장소명 또는 주소 검색 |
| `sort` | String | X | `createdAt,desc` | 정렬 조건 |

#### Response Body

```json
200 OK
{
  "data": {
    "places": [
      {
        "placeId": 1,
        "name": "어니언 성수",
        "address": "서울 성동구 성수동2가 277-135",
        "roadAddress": "서울 성동구 아차산로9길 8",
        "phone": "02-1644-1941",
        "placeUrl": "https://place.map.kakao.com/123456789"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## Test 시나리오

### 장소 검색 및 생성

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-01 | 신규 장소 검색 | 같은 `kakaoPlaceId`가 DB에 없음 | 장소 검색 API 호출 | Place 1건 생성, `200 OK` |
| P-02 | 여러 신규 장소 검색 | 검색 결과에 신규 장소 3개 존재 | 장소 검색 API 호출 | Place 3건 생성 |
| P-03 | 카카오 선택 필드가 빈 문자열 | 전화번호와 도로명 주소가 빈 문자열 | 장소 검색 API 호출 | 해당 필드는 `NULL`로 저장 |
| P-04 | 좌표 저장 | 카카오 응답에 문자열 `x`, `y` 존재 | 장소 검색 API 호출 | `x`는 경도, `y`는 위도로 저장 |
| P-05 | 생성 결과 응답 | 신규 장소가 정상 저장됨 | 응답 확인 | 생성된 내부 `placeId`가 반환됨 |
| P-06 | 검색 결과 없음 | 카카오 응답의 `documents`가 빈 배열 | 장소 검색 API 호출 | DB 변경 없이 빈 배열과 `200 OK` |

### 기존 장소 갱신

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-07 | 같은 장소 재검색 | 같은 `kakaoPlaceId`의 장소가 DB에 있음 | 장소 검색 API 재호출 | 새로운 행이 생성되지 않음 |
| P-08 | 장소명 변경 | 카카오의 장소명이 기존 DB 값과 다름 | 장소 검색 API 호출 | 기존 행의 `name`이 갱신됨 |
| P-09 | 전화번호 변경 | 카카오 전화번호가 변경됨 | 장소 검색 API 호출 | 기존 행의 `phone`이 갱신됨 |
| P-10 | 주소 및 좌표 변경 | 카카오 주소와 좌표가 변경됨 | 장소 검색 API 호출 | 기존 행의 주소와 좌표가 갱신됨 |
| P-11 | 기존 값이 빈 문자열로 변경 | 카카오가 전화번호를 빈 문자열로 반환 | 장소 검색 API 호출 | `phone`이 `NULL`로 갱신됨 |
| P-12 | 식별 정보 유지 | 기존 장소 재검색 | 갱신 전후 비교 | `placeId`, `kakaoPlaceId`, `createdAt` 유지 |
| P-13 | 갱신 시각 변경 | 기존 장소가 존재함 | 장소 검색 API 호출 | `updatedAt`이 최근 시각으로 변경됨 |

### 중복 및 동시성

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-14 | 동일 검색 반복 | 같은 검색을 여러 번 수행 | 검색 API 반복 호출 | `kakaoPlaceId`별로 한 행만 존재 |
| P-15 | 동일 장소 동시 생성 | DB에 장소가 없는 상태 | 동일 검색 요청 2개 동시 실행 | UNIQUE 제약 위반을 처리하고 한 행만 저장 |
| P-16 | 카카오 결과 내 중복 ID | 동일 응답에 같은 ID가 중복 포함됨 | 장소 검색 API 호출 | 한 행만 저장되고 응답도 중복 제거 |

### 소프트 삭제

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-17 | 장소 삭제 | 정상 장소가 존재함 | 관리자 삭제 실행 | `deletedAt`이 설정되고 실제 행은 유지 |
| P-18 | 삭제 후 단건 조회 | 장소가 소프트 삭제됨 | 단건 조회 API 호출 | `404 PLACE_NOT_FOUND` |
| P-19 | 삭제 후 목록 조회 | 장소가 소프트 삭제됨 | 목록 조회 API 호출 | 삭제된 장소가 목록에서 제외됨 |
| P-20 | 삭제 후 카카오 재검색 | 소프트 삭제된 장소가 검색 결과에 포함됨 | 검색 API 호출 | 새 행을 만들지 않고 응답에서 제외 |
| P-21 | 삭제 장소 자동 복구 방지 | 소프트 삭제된 장소가 존재함 | 검색 API 호출 | `deletedAt`이 유지됨 |
| P-22 | 중복 삭제 | 이미 소프트 삭제된 장소 | 관리자 삭제 재호출 | 상태 변화 없이 `204 No Content` |
| P-23 | 존재하지 않는 장소 삭제 | 해당 `placeId`가 없음 | 관리자 삭제 호출 | `404 PLACE_NOT_FOUND` |

### 장소 복구

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-24 | 삭제된 장소 복구 | `deletedAt`이 설정됨 | 관리자 복구 실행 | `deletedAt`이 `NULL`로 변경 |
| P-25 | 복구 후 조회 | 장소 복구 완료 | 단건 조회 API 호출 | `200 OK`와 장소 정보 반환 |
| P-26 | 삭제되지 않은 장소 복구 | 정상 장소가 존재함 | 복구 요청 | 상태 변화 없이 `204 No Content` |

### 검색 요청 검증

| ID | 시나리오 | 실행 | 예상 결과 |
| --- | --- | --- | --- |
| P-27 | 검색어 누락 | `query` 없이 검색 | `400 PLACE_QUERY_REQUIRED` |
| P-28 | 빈 검색어 | 공백만 포함된 `query` 전달 | `400 PLACE_QUERY_REQUIRED` |
| P-29 | 페이지 크기 초과 | `size=16` 전달 | `400 INVALID_PLACE_PAGE_SIZE` |
| P-30 | 페이지 크기 최소 미만 | `size=0` 전달 | `400 INVALID_PLACE_PAGE_SIZE` |
| P-31 | 위도 범위 초과 | `latitude=91` 전달 | `400 INVALID_LOCATION` |
| P-32 | 경도 범위 초과 | `longitude=181` 전달 | `400 INVALID_LOCATION` |
| P-33 | 좌표 일부만 전달 | 위도 또는 경도 하나만 전달 | `400 INVALID_LOCATION` |
| P-34 | 거리순 정렬에 좌표 누락 | `sort=distance`, 좌표 없음 | `400 INVALID_LOCATION` |
| P-35 | 카카오 API 장애 | 카카오 API가 오류 또는 시간 초과 반환 | `502 KAKAO_LOCAL_API_ERROR` |
| P-36 | 카카오 응답 형식 오류 | 필수 필드 `id` 또는 `place_name` 누락 | 잘못된 장소를 저장하지 않고 정의된 연동 오류 처리 |

### 사용자 조회

| ID | 시나리오 | 사전 조건 | 실행 | 예상 결과 |
| --- | --- | --- | --- | --- |
| P-37 | 정상 단건 조회 | 정상 장소 존재 | `GET /places/{placeId}` | `200 OK` |
| P-38 | 없는 장소 조회 | `placeId` 없음 | 단건 조회 | `404 PLACE_NOT_FOUND` |
| P-39 | 정상 목록 조회 | 정상 장소 여러 개 존재 | `GET /places` | 삭제되지 않은 장소만 페이징 반환 |
| P-40 | 응답 필드 제한 | 정상 장소 존재 | 검색·단건·목록 응답 확인 | 카테고리와 좌표 등 내부 필드가 제외됨 |
| P-41 | 목록 키워드 검색 | 장소명 또는 주소와 일치하는 장소 존재 | `keyword` 전달 | 일치하는 정상 장소만 반환 |
| P-42 | 잘못된 정렬값 | 허용하지 않은 `sort` 전달 | 목록 API 호출 | `400` 요청 오류 |

## 내부 서비스 명세

### 내부 작업 목록

- errorcode
    
    
    | 상태 | 오류 코드 | 조건 |
    | --- | --- | --- |
    | 502 | `INVALID_KAKAO_PLACE_DATA` | 카카오 필수 데이터 누락 또는 좌표 오류 |
    | 404 | `PLACE_NOT_FOUND` | 내부 `placeId`에 해당하는 장소 없음 |
    | 500 | `PLACE_SYNC_FAILED` | 장소 저장 또는 갱신 실패 |
    | 500 | `KAKAO_PLACE_ID_CONFLICT` | 동시 저장 중 UNIQUE 충돌 처리 실패 |
- 내부 처리 결과 모델
    
    생성과 갱신 여부를 로깅하거나 테스트해야 한다면 결과 타입을 별도로 정의할 수 있다.
    
    ```
    record PlaceSyncResult(
        Place place,
        PlaceSyncStatus status
    ) {}
    ```
    
    ```
    enum PlaceSyncStatus {
        CREATED,
        UPDATED,
        SKIPPED_DELETED,
        SKIPPED_INVALID
    }
    ```
    
    처리 결과의 의미:
    
    | 상태 | 설명 |
    | --- | --- |
    | `CREATED` | 새로운 Place 생성 |
    | `UPDATED` | 기존 Place 갱신 |
    | `SKIPPED_DELETED` | 소프트 삭제된 장소이므로 제외 |
    | `SKIPPED_INVALID` | 카카오 응답이 유효하지 않아 제외 |
    
    이 결과는 사용자 API 응답에 노출하지 않고 내부 로그와 테스트에서만 사용한다.
    

| 작업명 | 실행 시점 | 주요 처리 |
| --- | --- | --- |
| `synchronizePlace` | 카카오 검색 결과 수신 시 | 장소 한 건 생성 또는 갱신 |
| `synchronizePlaces` | 카카오 검색 결과 목록 수신 시 | 여러 장소 생성 또는 갱신 |
| `softDeletePlace` | 관리자 삭제 또는 내부 삭제 조건 발생 시 | `deletedAt` 설정 |
| `restorePlace` | 관리자가 장소 복구 시 | `deletedAt` 초기화 |
| `findActivePlace` | 장소 단건 조회 시 | 삭제되지 않은 장소 조회 |

#### 장소 생성·갱신

## 3.1 작업명

```
synchronizePlace
```

## 3.2 목적

카카오 Local API에서 받은 장소 한 건을 내부 Place 데이터로 변환한다.

`kakaoPlaceId`가 존재하지 않으면 신규 장소를 생성하고, 이미 존재하면 기존 장소 정보를 최신 값으로 갱신한다.

## 3.3 호출 시점

```
GET /api/v1/places/search
        ↓
카카오 Local API 호출
        ↓
카카오 검색 결과 수신
        ↓
각 document에 대해 synchronizePlace 실행
```

이 작업은 클라이언트가 직접 호출할 수 없다.

## 3.4 입력

입력값은 카카오 Local API의 장소 검색 결과 한 건이다.

```
{
  "id": "123456789",
  "place_name": "어니언 성수",
  "category_name": "음식점 > 카페",
  "address_name": "서울 성동구 성수동2가 277-135",
  "road_address_name": "서울 성동구 아차산로9길 8",
  "x": "127.0583769",
  "y": "37.5446891",
  "phone": "02-1644-1941",
  "place_url": "https://place.map.kakao.com/123456789"
}
```

서버 코드의 입력 모델 예시:

```
KakaoPlaceDocument
```

## 3.5 입력 검증

| 필드 | 검증 조건 | 실패 처리 |
| --- | --- | --- |
| `id` | 필수, 빈 문자열 불가 | 해당 장소 저장 제외 |
| `place_name` | 필수, 빈 문자열 불가 | 해당 장소 저장 제외 |
| `x` | 값이 있다면 경도 범위 `-180~180` | 해당 장소 저장 제외 |
| `y` | 값이 있다면 위도 범위 `-90~90` | 해당 장소 저장 제외 |
| 선택 필드 | 빈 문자열 허용 | `NULL`로 변환 |

`id` 또는 `place_name`이 없는 응답은 정상적인 Place로 저장할 수 없으므로 제외한다.

## 3.6 처리 순서

```
1. 카카오 응답 필수값 검증
2. 빈 문자열을 NULL로 변환
3. x를 longitude로 변환
4. y를 latitude로 변환
5. kakaoPlaceId로 기존 Place 조회
6. 기존 Place가 없으면 생성
7. 기존 Place가 있으면 갱신
8. 저장된 Place 반환
```

## 3.7 신규 생성 조건

```
kakao_place_id = 입력된 id
```

위 조건에 해당하는 Place가 DB에 없으면 신규 생성한다.

생성 시 설정되는 필드:

| 필드 | 값 |
| --- | --- |
| `placeId` | DB 자동 생성 |
| `kakaoPlaceId` | 카카오 `id` |
| `name` | 카카오 `place_name` |
| `category` | 카카오 `category_name` |
| `address` | 카카오 `address_name` |
| `roadAddress` | 카카오 `road_address_name` |
| `latitude` | 카카오 `y` |
| `longitude` | 카카오 `x` |
| `phone` | 카카오 `phone` |
| `placeUrl` | 카카오 `place_url` |
| `createdAt` | 현재 시각 |
| `updatedAt` | 현재 시각 |
| `deletedAt` | `NULL` |

## 3.8 기존 장소 갱신 조건

동일한 `kakaoPlaceId`의 Place가 존재하면 기존 행을 갱신한다.

갱신되는 필드:

- `name`
- `category`
- `address`
- `roadAddress`
- `latitude`
- `longitude`
- `phone`
- `placeUrl`
- `updatedAt`

유지되는 필드:

- `placeId`
- `kakaoPlaceId`
- `createdAt`
- `deletedAt`

## 3.9 소프트 삭제된 장소

동일한 `kakaoPlaceId`의 장소가 소프트 삭제된 상태라면:

- 새로운 Place를 생성하지 않는다.
- `deletedAt`을 `NULL`로 변경하지 않는다.
- 사용자 검색 결과에서 제외한다.
- 카카오 최신 정보 갱신 여부는 정책에 따라 결정한다.

권장 정책은 삭제된 장소의 다른 정보는 갱신하지 않고 그대로 유지하는 것이다.

```
deletedAt != null
→ 갱신하지 않음
→ 검색 응답에서 제외
```

삭제된 데이터를 계속 갱신할 필요가 없고, 삭제 당시 상태를 보존할 수 있기 때문이다.

## 3.10 반환값

내부 서비스는 저장된 Place를 반환한다.

```
Place
```

검색 응답을 만들 때는 내부 Place를 사용자 응답 DTO로 변환한다.

```
Place
  → PlaceSearchResponse
```

## 3.11 트랜잭션

장소 한 건의 조회·생성·갱신은 하나의 트랜잭션에서 처리한다.

```
@Transactional
Place synchronizePlace(KakaoPlaceDocument document)
```

카카오 API 호출 자체는 DB 트랜잭션 밖에서 수행하는 것을 권장한다.

```
카카오 API 호출
    ↓
응답 수신
    ↓
DB 트랜잭션 시작
    ↓
Place 생성·갱신
    ↓
트랜잭션 종료
```

외부 API 응답을 기다리는 동안 DB 트랜잭션이 불필요하게 유지되는 것을 방지하기 위함이다.

#### 여러 장소 일괄 동기화

## 4.1 작업명

```
synchronizePlaces
```

## 4.2 입력

```
List<KakaoPlaceDocument>
```

## 4.3 처리

카카오 검색 결과의 각 장소를 `kakaoPlaceId` 기준으로 생성하거나 갱신한다.

```
documents
    ↓
필수값 검증
    ↓
중복 kakaoPlaceId 제거
    ↓
기존 Place 일괄 조회
    ↓
신규 Place 생성
    ↓
기존 Place 갱신
    ↓
삭제된 Place 제외
    ↓
사용자 응답 반환
```

검색 결과를 한 건씩 조회하면 DB 요청이 반복될 수 있으므로, 다음과 같이 카카오 ID 목록으로 기존 장소를 한 번에 조회하는 방식을 권장한다.

```
findAllByKakaoPlaceIdIn(kakaoPlaceIds)
```

## 4.4 일부 데이터 처리 실패

검색 결과 중 일부 장소만 잘못된 경우 전체 검색을 실패시키지 않고 해당 장소만 제외한다.

예:

```
15건 수신
→ 정상 14건 저장·갱신
→ 필수 ID가 없는 1건 제외
→ 정상 14건 반환
```

다만 DB 연결 실패처럼 전체 저장이 불가능한 경우에는 검색 요청을 실패 처리한다.

## 4.5 반환값

```
List<Place>
```

소프트 삭제된 장소와 유효하지 않은 장소는 반환 목록에서 제외한다.

#### 장소 소프트 삭제

## 5.1 작업명

```
softDeletePlace
```

## 5.2 목적

Place 행을 실제로 제거하지 않고 삭제 시각을 기록한다.

## 5.3 호출 주체

다음 중 하나로 제한한다.

- 관리자 API
- 운영 도구
- 관리 배치
- 다른 서버 내부 서비스

일반 사용자는 직접 호출할 수 없다.

## 5.4 입력

```
placeId: Long
```

위도와 경도는 삭제 대상 식별에 사용하지 않는다.

## 5.5 처리 순서

```
1. placeId로 Place 조회
2. 장소가 없으면 PLACE_NOT_FOUND
3. 이미 삭제된 장소면 추가 변경 없이 종료
4. deletedAt에 현재 시각 설정
5. updatedAt 갱신
```

## 5.6 처리 결과

| 조건 | 내부 결과 |
| --- | --- |
| 정상 장소 | `deletedAt` 설정 |
| 이미 삭제된 장소 | 상태 변경 없음 |
| 존재하지 않는 장소 | `PLACE_NOT_FOUND` 발생 |

내부 서비스 예시:

```
@Transactional
void softDeletePlace(Long placeId)
```

#### 장소 복구

## 6.1 작업명

```
restorePlace
```

## 6.2 입력

```
placeId: Long
```

## 6.3 처리 순서

```
1. 삭제 여부와 관계없이 placeId로 Place 조회
2. 장소가 없으면 PLACE_NOT_FOUND
3. deletedAt이 NULL이면 추가 변경 없이 종료
4. deletedAt을 NULL로 변경
5. updatedAt 갱신
```

내부 서비스 예시:

```
@Transactional
void restorePlace(Long placeId)
```

#### 활성 장소 조회

## 7.1 작업명

```
findActivePlace
```

## 7.2 입력

```
placeId: Long
```

## 7.3 조회 조건

```
placeId 일치
AND deletedAt IS NULL
```

## 7.4 반환 및 오류

| 조건 | 결과 |
| --- | --- |
| 활성 장소 존재 | Place 반환 |
| 장소 없음 | `PLACE_NOT_FOUND` |
| 소프트 삭제됨 | `PLACE_NOT_FOUND` |

일반 사용자에게는 장소가 존재하지 않는 경우와 삭제된 경우를 구분해서 노출하지 않는다.

### 내부 서비스 테스트 시나리오

| ID | 대상 작업 | 조건 | 예상 결과 |
| --- | --- | --- | --- |
| IS-01 | `synchronizePlace` | 신규 `kakaoPlaceId` | `CREATED`, Place 1건 저장 |
| IS-02 | `synchronizePlace` | 기존 `kakaoPlaceId` | `UPDATED`, 기존 행 갱신 |
| IS-03 | `synchronizePlace` | 기존 장소 재검색 | `placeId`와 `createdAt` 유지 |
| IS-04 | `synchronizePlace` | 카카오 ID 누락 | `SKIPPED_INVALID`, 저장 안 됨 |
| IS-05 | `synchronizePlace` | 장소명 누락 | `SKIPPED_INVALID`, 저장 안 됨 |
| IS-06 | `synchronizePlace` | 선택값이 빈 문자열 | `NULL`로 저장 |
| IS-07 | `synchronizePlace` | 삭제된 장소 재검색 | `SKIPPED_DELETED`, 자동 복구 안 됨 |
| IS-08 | `synchronizePlaces` | 동일 ID 중복 포함 | 중복 제거 후 한 번만 처리 |
| IS-09 | `synchronizePlaces` | 정상 2건, 오류 1건 | 정상 2건 처리, 오류 1건 제외 |
| IS-10 | `synchronizePlaces` | 기존 장소 여러 건 | 한 번의 일괄 조회 후 갱신 |
| IS-11 | `softDeletePlace` | 활성 장소 | `deletedAt` 설정 |
| IS-12 | `softDeletePlace` | 이미 삭제된 장소 | 추가 상태 변경 없이 정상 종료 |
| IS-13 | `softDeletePlace` | 존재하지 않는 장소 | `PLACE_NOT_FOUND` |
| IS-14 | `restorePlace` | 삭제된 장소 | `deletedAt`이 `NULL`로 변경 |
| IS-15 | `restorePlace` | 활성 장소 | 상태 변경 없이 정상 종료 |
| IS-16 | `findActivePlace` | 활성 장소 | Place 반환 |
| IS-17 | `findActivePlace` | 삭제된 장소 | `PLACE_NOT_FOUND` |
| IS-18 | 동시 저장 | 같은 ID를 동시에 저장 | DB에는 한 행만 존재 |
