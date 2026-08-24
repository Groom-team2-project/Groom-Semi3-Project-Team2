# Schedule

## 구현 결정 사항

- 데이터베이스와 API 요청의 기준 모델은 `startAt`, `placeId`, `reservationStatus`를 사용한다.
- 모든 API 응답은 프로젝트 공통 형식인 `CommonResponse<T>`로 감싼다. 아래 API별 JSON 예시는 `data`에 들어가는 값만 표현한다.
- 조회는 해당 Plan의 `JOINED` 멤버에게 허용한다.
- 생성·수정·삭제·순서 변경은 `OWNER`와 `EDITOR`에게만 허용한다. `VIEWER`의 변경 요청은 `PLAN_UPDATE_FORBIDDEN`으로 처리한다.
- 기존 V1 마이그레이션은 수정하지 않고 신규 마이그레이션에서 새 스키마로 변경한다.
- 장소가 없는 일정도 지원하므로 `placeId`와 응답의 `place`는 `null`일 수 있다.

### 공통 응답 형식

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "일정 조회 성공"
}
```

## Entity

### 사용자 요청 필드

| 컬럼                   | 타입 | 제약 | 설명 |
|----------------------| --- | --- | --- |
| `schedule_id`        | BIGINT | PK, AUTO_INCREMENT | 일정 식별자 |
| `plan_id`            | BIGINT | FK → `plans.plan_id`, NOT NULL | 일정이 속한 여행 계획 |
| `place_id`           | BIGINT | FK → `places.place_id`, NULL | 선택적 참조. `null`이면 자유시간, 숙소 체크아웃 등 장소 없는 일정 가능 |
| `created_at`         | DATETIME(6) | NOT NULL | 생성 시각 |
| `updated_at`         | DATETIME(6) | NOT NULL | 마지막 수정 시각 |
| `deleted_at`         | DATETIME(6) | NULL | 소프트 삭제 시각 |
| `title`              | VARCHAR(200) | NOT NULL | 일정 제목 |
| `sort_order`         | INT | NOT NULL | 같은 여행 계획 내 표시 순서 |
| `memo`               | VARCHAR(1000) | NULL | 일정 메모 |
| `start_at`           | DATETIME | NOT NULL | 일정 시작 일시 |
| `end_at`              | DATETIME | NULL | 일정 종료 시간 |
| `reservation_status` | ENUM | NOT NULL, DEFAULT `NOT_REQUIRED` | `NOT_REQUIRED`(예약 불필요), `UNRESERVED`(미예약), `RESERVED`(예약 완료), `CANCELLED`(예약 취소) |
| `kakao_route_url`    | VARCHAR(500) | NULL | 카카오맵 길찾기 딥링크(동선 기능 P2 대체용, API 승인 불필요) |

## API 명세

### POST `/api/v1/plans/{planId}/schedules`

특정 여행 계획에 새로운 일정을 생성한다. `sortOrder`는 서비스에서 해당 여행 계획의 마지막 순서로 자동 지정한다.

`startAt`은 필수이며 날짜와 시간을 포함한 ISO 8601 형식으로 전달한다.

#### Request Body

```json
{
  "placeId": 15,
  "title": "성산일출봉 관람",
  "startAt": "2026-08-15T09:00:00",
  "endTime": "11:00",
  "memo": "입장권 확인",
  "reservationStatus": "NOT_REQUIRED",
  "kakaoRouteUrl": "https://map.kakao.com/..."
}
```

#### Response Body

```json
{
  "scheduleId": 31,
  "planId": 1,
  "place": {
    "placeId": 15,
    "name": "성산일출봉",
    "address": "제주특별자치도 서귀포시 성산읍 성산리 1번지",
    "phone": null,
    "placeUrl": null
  },
  "title": "성산일출봉 관람",
  "sortOrder": 4,
  "startAt": "2026-08-15T09:00:00",
  "endTime": "11:00",
  "memo": "입장권 확인",
  "reservationStatus": "NOT_REQUIRED",
  "kakaoRouteUrl": "https://map.kakao.com/...",
  "createdAt": "2026-08-11T15:30:00.123456",
  "updatedAt": "2026-08-11T15:30:00.123456"
}
```

성공 상태: `201 Created`

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 제목 또는 `startAt`이 누락되었거나 길이·일시 형식이 잘못됨 |
| 400 | `INVALID_TIME_RANGE` | 종료 시간이 시작 시간과 같거나 빠름 |
| 400 | `INVALID_RESERVATION_STATUS` | 지원하지 않는 예약 상태 |
| 403 | `PLAN_ACCESS_DENIED` | 해당 여행 계획에 접근 권한이 없음 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 404 | `PLACE_NOT_FOUND` | Place가 없거나 삭제됨 |

### GET `/api/v1/plans/{planId}/schedules`

특정 여행 계획의 삭제되지 않은 일정 목록을 조회한다.

#### Response Body

일정이 없는 경우에도 `200 OK`와 빈 배열을 반환한다.

```json
{
  "planId": 1,
  "schedules": []
}
```

일정이 있는 경우 `sortOrder` 오름차순으로 반환한다.

```json
{
  "planId": 1,
  "schedules": [
    {
      "scheduleId": 30,
      "place": null,
      "title": "숙소 체크아웃",
      "sortOrder": 1,
      "startAt": "2026-08-15T08:30:00",
      "endTime": null,
      "reservationStatus": "NOT_REQUIRED"
    },
    {
      "scheduleId": 31,
      "place": {
        "placeId": 15,
        "name": "성산일출봉",
        "address": "제주특별자치도 서귀포시 성산읍 성산리 1번지",
        "phone": null,
        "placeUrl": null
      },
      "title": "성산일출봉 관람",
      "sortOrder": 2,
      "startAt": "2026-08-15T09:00:00",
      "endTime": "11:00",
      "reservationStatus": "NOT_REQUIRED"
    }
  ]
}
```

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 여행 계획 접근 권한 없음 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |

### GET `/api/v1/plans/{planId}/schedules/{scheduleId}`

일정을 상세 조회한다. 삭제된 일정까지 조회하려면 `includeDeleted=true`를 전달한다.

```http
GET /api/v1/schedules/31?includeDeleted=true
```

#### Response Body

```json
{
  "scheduleId": 31,
  "planId": 1,
  "place": {
    "placeId": 15,
    "name": "성산일출봉",
    "address": "제주특별자치도 서귀포시 성산읍 성산리 1번지",
    "phone": null,
    "placeUrl": null
  },
  "title": "성산일출봉 관람",
  "sortOrder": 2,
  "startAt": "2026-08-15T09:00:00",
  "endTime": "11:00",
  "memo": "입장권 확인",
  "reservationStatus": "NOT_REQUIRED",
  "kakaoRouteUrl": "https://map.kakao.com/...",
  "createdAt": "2026-08-11T15:30:00.123456",
  "updatedAt": "2026-08-11T15:30:00.123456",
  "deletedAt": null
}
```

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 여행 계획 접근 권한 없음 |
| 404 | `SCHEDULE_NOT_FOUND` | 일정이 없거나, 일반 조회에서 삭제된 일정인 경우 |

### PATCH `/api/v1/plans/{planId}/schedules/{scheduleId}`

전달된 필드만 수정한다. 일정 순서는 이 API에서 변경하지 않는다.

필드 생략과 명시적인 `null`을 안정적으로 구분하기 위해 장소 연결 및 메모 삭제에는 `clearPlace`, `clearMemo`를 사용한다.

#### Request Body

```json
{
  "title": "성산일출봉 일출 관람",
  "startAt": "2026-08-15T06:00:00",
  "endTime": "08:00",
  "clearMemo": true,
  "clearPlace": false,
  "reservationStatus": "RESERVED"
}
```

#### PATCH 필드 처리 규칙

| 요청 | 처리 |
| --- | --- |
| 필드 생략 | 기존 값 유지 |
| `placeId` 전달 | 활성 Place 확인 후 장소 변경 |
| `clearPlace: true` | 장소 연결 해제 |
| `memo` 전달 | 메모 변경 |
| `clearMemo: true` | 메모 삭제 |
| `placeId`와 `clearPlace: true` 동시 전달 | `INVALID_REQUEST` |
| `memo`와 `clearMemo: true` 동시 전달 | `INVALID_REQUEST` |

#### Response Body

```json
{
  "scheduleId": 31,
  "planId": 1,
  "place": {
    "placeId": 15,
    "name": "성산일출봉",
    "address": "제주특별자치도 서귀포시 성산읍 성산리 1번지",
    "phone": null,
    "placeUrl": null
  },
  "title": "성산일출봉 일출 관람",
  "sortOrder": 2,
  "startAt": "2026-08-15T06:00:00",
  "endTime": "08:00",
  "memo": null,
  "reservationStatus": "RESERVED",
  "kakaoRouteUrl": "https://map.kakao.com/...",
  "createdAt": "2026-08-11T15:30:00.123456",
  "updatedAt": "2026-08-11T16:10:00.123456"
}
```

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 수정값 검증 실패 |
| 400 | `INVALID_TIME_RANGE` | 수정 결과의 시간 범위가 잘못됨 |
| 400 | `INVALID_RESERVATION_STATUS` | 지원하지 않는 예약 상태 |
| 403 | `PLAN_ACCESS_DENIED` | 여행 계획 접근 권한 없음 |
| 404 | `SCHEDULE_NOT_FOUND` | 일정이 없거나 삭제됨 |
| 404 | `PLACE_NOT_FOUND` | 변경할 Place가 없거나 삭제됨 |

### PATCH `/api/v1/plans/{planId}/schedules/order`

여러 일정의 순서를 변경한다. 생성 시에는 서비스가 마지막 순서를 자동 지정하고, 사용자가 순서를 직접 변경할 때 이 API를 사용한다.

#### Request Body

```json
{
  "scheduleIds": [33, 31, 32]
}
```

#### 검증 규칙

- 요청 배열에는 해당 Plan의 모든 활성 Schedule ID가 한 번씩 포함되어야 한다.
- 중복된 ID를 포함할 수 없다.
- 다른 Plan에 속한 Schedule ID를 포함할 수 없다.
- 삭제된 Schedule ID를 포함할 수 없다.

#### Response Body

```json
{
  "planId": 1,
  "schedules": [
    {
      "scheduleId": 33,
      "sortOrder": 1
    },
    {
      "scheduleId": 31,
      "sortOrder": 2
    },
    {
      "scheduleId": 32,
      "sortOrder": 3
    }
  ]
}
```

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_SCHEDULE_ORDER` | 누락, 중복 또는 다른 여행 계획의 일정 포함 |
| 403 | `PLAN_ACCESS_DENIED` | 여행 계획 접근 권한 없음 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |

### DELETE `/api/v1/plans/{planId}/schedules/{scheduleId}`

일정을 소프트 삭제한다. 실제 레코드는 제거하지 않고 `deleted_at`에 삭제 시각을 기록하며, 남은 일정의 `sortOrder`를 서비스에서 재정렬한다.

#### Response Body

성공 상태: `200 OK`

```json
{
  "scheduleId": 31,
  "deletedAt": "2026-08-11T17:00:00.123456"
}
```

#### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 여행 계획 접근 권한 없음 |
| 404 | `SCHEDULE_NOT_FOUND` | 일정이 없거나 이미 삭제됨 |

## Test 시나리오

### 일정 생성

```text
Given 존재하며 접근 가능한 Plan이 있고
When 유효한 일정 생성 요청을 보내면
Then 일정이 마지막 순서로 생성된다.
```

```text
Given 존재하는 Plan이 있고
When placeId 없이 일정을 생성하면
Then 장소 없는 일정이 정상적으로 생성된다.
```

```text
Given 종료 시간이 시작 시간보다 빠른 요청이 있고
When 일정을 생성하면
Then INVALID_TIME_RANGE 오류가 발생한다.
```

```text
Given startAt이 없는 요청이 있고
When 일정을 생성하면
Then INVALID_REQUEST 오류가 발생한다.
```

### 일정 조회

```text
Given 활성 일정과 삭제된 일정이 함께 있고
When Plan의 일정 목록을 조회하면
Then 삭제되지 않은 일정만 순서대로 반환된다.
```

```text
Given Plan에 일정이 없고
When 목록을 조회하면
Then 빈 배열과 200 OK를 반환한다.
```

```text
Given 삭제된 일정이 있고
When includeDeleted=true로 단건 조회하면
Then deletedAt을 포함한 일정 정보를 반환한다.
```

### 일정 수정

```text
Given 기존 일정이 있고
When title만 전달하여 PATCH 요청하면
Then title만 변경되고 나머지 필드는 유지된다.
```

```text
Given 메모가 등록된 일정이 있고
When clearMemo를 true로 전달하면
Then 메모가 제거된다.
```

```text
Given 장소가 연결된 일정이 있고
When clearPlace를 true로 전달하면
Then 장소 연결이 제거된다.
```

### 순서 변경

```text
Given 세 개의 일정이 있고
When 새로운 ID 순서로 변경을 요청하면
Then 모든 일정의 sortOrder가 요청 순서대로 변경된다.
```

```text
Given 다른 Plan의 Schedule ID가 포함되어 있고
When 순서 변경을 요청하면
Then 전체 변경이 취소되고 INVALID_SCHEDULE_ORDER가 발생한다.
```

### 일정 삭제

```text
Given 활성 일정이 있고
When 삭제를 요청하면
Then 200 OK와 scheduleId 및 deletedAt을 반환한다.
```

```text
Given 삭제된 일정이 있고
When 목록 조회를 요청하면
Then 해당 일정은 조회되지 않는다.
```

```text
Given 이미 삭제된 일정이 있고
When 다시 삭제를 요청하면
Then SCHEDULE_NOT_FOUND 오류가 발생한다.
```
