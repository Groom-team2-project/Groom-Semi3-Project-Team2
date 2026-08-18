# Plan

## 1. 목적

Plan 도메인은 사용자가 여행 계획을 만들고, 다른 사용자와 함께 계획을 관리할 수 있도록 지원한다.

하나의 Plan을 기준으로 참여 멤버와 역할을 관리하고, 초대 링크를 통해 새로운 사용자가 계획에 참여할 수 있도록 한다.

## 2. 기능 범위

### 2.1 Plan

- Plan 생성
- 내가 참여 중인 Plan 목록 조회
- Plan 상세 조회
- Plan 수정
- Plan 삭제
- 여행 기간과 모집 인원 관리

### 2.2 Member

- Plan 참여 멤버 목록 조회
- 멤버 역할 관리 (`OWNER`, `EDITOR`, `VIEWER`)
- 멤버 내보내기
- 본인 Plan 나가기
- 탈퇴한 멤버의 초대 링크 재참여

### 2.3 Invitation

- Plan 초대 링크 발급
- 초대 링크 재발급
- 초대 링크 취소
- 초대 코드 정보 조회
- 초대 코드를 이용한 Plan 참여
- 초대 링크 만료 상태 관리

## 3. 공통 응답 형식

```json
{
  "success": true,
  "data": {},
  "errorCode": null,
  "message": "계획 조회 성공"
}
```

오류 응답 예시:

```json
{
  "success": false,
  "data": null,
  "errorCode": "PLAN_NOT_FOUND",
  "message": "계획을 찾을 수 없습니다."
}
```

## 4. 권한 정책

| 기능 | OWNER | EDITOR | VIEWER | 비참여자 |
| --- | --- | --- | --- | --- |
| 내가 참여 중인 Plan 목록 조회 | O | O | O | 본인이 참여한 Plan만 조회 |
| Plan 상세 조회 | O | O | O | X |
| Plan 수정 | O | O | X | X |
| Plan 삭제 | O | X | X | X |
| 멤버 목록 조회 | O | O | O | X |
| 멤버 권한 변경 | O | X | X | X |
| 멤버 내보내기 | O | X | X | X |
| 본인 Plan 나가기 | X | O | O | X |
| 초대 링크 발급 | O | X | X | X |
| 초대 링크 재발급 | O | X | X | X |
| 초대 링크 취소 | O | X | X | X |
| 초대 코드 조회 | 인증 필요 | 인증 필요 | 인증 필요 | 인증된 사용자라면 가능 |
| 초대 코드로 참여 | 이미 참여 중이면 X | 이미 참여 중이면 X | 이미 참여 중이면 X | O |

## 5. 데이터 모델

### 5.1 Plan

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `plan_id` | BIGINT | PK, AUTO_INCREMENT | Plan 식별자 |
| `user_id` | BIGINT | FK → `users.user_id`, NOT NULL | Plan 최초 생성자(소유자) |
| `title` | VARCHAR(100) | NOT NULL | 계획 제목 |
| `description` | VARCHAR(1000) | NULL | 계획 설명 |
| `start_date` | DATE | NOT NULL | 여행 시작일 |
| `end_date` | DATE | NOT NULL | 여행 종료일 |
| `recruitment_count` | INT | NULL | 모집 인원. `NULL`이면 인원 제한 없음 |
| `created_at` | DATETIME(6) | NOT NULL | 생성 시각 |
| `updated_at` | DATETIME(6) | NOT NULL | 마지막 수정 시각 |
| `deleted_at` | DATETIME(6) | NULL | 소프트 삭제 시각 |

### 5.2 Member

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `member_id` | BIGINT | PK, AUTO_INCREMENT | 멤버 식별자 |
| `plan_id` | BIGINT | FK → `plans.plan_id`, NOT NULL | 참여 Plan |
| `user_id` | BIGINT | FK → `users.user_id`, NOT NULL | 참여 사용자 |
| `role` | ENUM | NOT NULL | `OWNER`, `EDITOR`, `VIEWER` |
| `status` | ENUM | NOT NULL, DEFAULT `JOINED` | `JOINED`, `LEFT` |
| `joined_at` | DATETIME(6) | NULL | 최근 참여 시각 |

제약:

```text
UNIQUE(plan_id, user_id)
```

동일 사용자는 같은 Plan에 Member 행을 두 개 이상 가질 수 없다.

### 5.3 Invitation

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `invitation_id` | BIGINT | PK, AUTO_INCREMENT | 초대 링크 식별자 |
| `plan_id` | BIGINT | FK → `plans.plan_id`, NOT NULL | 초대 대상 Plan |
| `inviter_id` | BIGINT | FK → `users.user_id` | 초대 링크 발급자 |
| `invite_code` | VARCHAR(20) | NOT NULL, UNIQUE | 초대 코드. 현재 생성 길이는 8자 |
| `status` | ENUM | NOT NULL, DEFAULT `ACTIVE` | `ACTIVE`, `EXPIRED`, `REVOKED` |
| `expires_at` | DATETIME(6) | NOT NULL | 만료 시각 |
| `created_at` | DATETIME(6) | NOT NULL | 생성 시각 |

> V2 마이그레이션에서는 기존 데이터 호환을 위해 `inviter_id`가 DB상 `NULL` 허용으로 추가되어 있으나, 현재 JPA Entity와 신규 생성 로직은 발급자를 필수값으로 사용한다.

## 6. 상태 및 역할 값

### 6.1 MemberRole

| 값 | 설명 |
| --- | --- |
| `OWNER` | Plan 소유자. Plan 삭제, 멤버 관리, 초대 링크 관리 가능 |
| `EDITOR` | Plan 조회 및 수정 가능 |
| `VIEWER` | Plan 조회만 가능 |

### 6.2 MemberStatus

| 값 | 설명 |
| --- | --- |
| `JOINED` | 현재 Plan에 참여 중 |
| `LEFT` | 나가기 또는 내보내기로 현재는 참여하지 않음 |

### 6.3 InvitationStatus

| 값 | 설명 |
| --- | --- |
| `ACTIVE` | 사용 가능한 초대 링크 |
| `EXPIRED` | 유효기간이 지난 초대 링크 |
| `REVOKED` | OWNER가 취소했거나 재발급·Plan 삭제 과정에서 무효화된 링크 |

---

## 7. API 명세

### 7.1 Plan API

#### POST `/api/v1/plans`

새로운 여행 계획을 생성한다.

계획을 생성한 사용자는 자동으로 Member에 `OWNER`, `JOINED` 상태로 등록된다.

##### Request Body

```json
{
  "title": "제주도 3박 4일",
  "description": "여름 제주 여행",
  "startDate": "2026-08-15",
  "endDate": "2026-08-18",
  "recruitmentCount": 4
}
```

##### 요청 필드

| 필드 | 타입 | 필수 | 검증 | 설명 |
| --- | --- | --- | --- | --- |
| `title` | String | O | 공백 불가, 최대 100자 | 계획 제목 |
| `description` | String | X | 최대 1000자 | 계획 설명 |
| `startDate` | LocalDate | O | `YYYY-MM-DD` | 시작일 |
| `endDate` | LocalDate | O | `YYYY-MM-DD`, `startDate` 이상 | 종료일 |
| `recruitmentCount` | Integer | X | 1 이상 | 모집 인원. 생략 또는 `null`이면 무제한 |

##### 처리 순서

```text
1. 인증 사용자 확인
2. 요청값 Bean Validation
3. 사용자 조회
4. 제목·기간·모집 인원 도메인 검증
5. Plan 저장
6. 생성자를 OWNER Member로 저장
7. Plan 응답 반환
```

Plan 생성과 OWNER Member 생성은 하나의 트랜잭션에서 처리한다.

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": {
    "planId": 1,
    "title": "제주도 3박 4일",
    "description": "여름 제주 여행",
    "startDate": "2026-08-15",
    "endDate": "2026-08-18",
    "recruitmentCount": 4,
    "ownerId": 10,
    "myRole": "OWNER",
    "memberCount": 1,
    "createdAt": "2026-08-18T20:00:00.123456"
  },
  "errorCode": null,
  "message": "계획 생성 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_INPUT_VALUE` | DTO 검증 실패: 빈 제목, 최대 길이 초과, 날짜 누락, 모집 인원 0 이하 등 |
| 400 | `INVALID_PLAN_DATE` | 시작일이 종료일보다 늦음 |
| 400 | `INVALID_PLAN_TITLE` | 서비스 계층에서 제목 검증 실패 |
| 400 | `INVALID_RECRUITMENT_COUNT` | 서비스 계층에서 모집 인원 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 사용자 조회 실패 |

---

#### GET `/api/v1/plans`

현재 로그인한 사용자가 `JOINED` 상태로 참여 중인 Plan 목록을 조회한다.

삭제된 Plan과 `LEFT` 상태로 나간 Plan은 반환하지 않는다.

##### 정렬

```text
createdAt DESC
```

최근 생성된 Plan이 먼저 반환된다.

##### Response Body

Plan이 없는 경우에도 `200 OK`와 빈 배열을 반환한다.

```json
{
  "success": true,
  "data": [],
  "errorCode": null,
  "message": "계획 목록 조회 성공"
}
```

Plan이 있는 경우:

```json
{
  "success": true,
  "data": [
    {
      "planId": 2,
      "title": "부산 여행",
      "description": null,
      "startDate": "2026-09-10",
      "endDate": "2026-09-12",
      "recruitmentCount": null,
      "ownerId": 20,
      "myRole": "EDITOR",
      "memberCount": 3,
      "createdAt": "2026-08-18T20:10:00.123456"
    },
    {
      "planId": 1,
      "title": "제주도 3박 4일",
      "description": "여름 제주 여행",
      "startDate": "2026-08-15",
      "endDate": "2026-08-18",
      "recruitmentCount": 4,
      "ownerId": 10,
      "myRole": "OWNER",
      "memberCount": 2,
      "createdAt": "2026-08-18T20:00:00.123456"
    }
  ],
  "errorCode": null,
  "message": "계획 목록 조회 성공"
}
```

각 Plan의 `memberCount`는 현재 `JOINED` 상태 멤버 수를 의미한다.

---

#### GET `/api/v1/plans/{planId}`

특정 Plan의 상세 정보를 조회한다.

해당 Plan에 `JOINED` 상태로 참여 중인 사용자만 조회할 수 있다.

##### Response Body

```json
{
  "success": true,
  "data": {
    "planId": 1,
    "title": "제주도 3박 4일",
    "description": "여름 제주 여행",
    "startDate": "2026-08-15",
    "endDate": "2026-08-18",
    "recruitmentCount": 4,
    "ownerId": 10,
    "myRole": "VIEWER",
    "memberCount": 4,
    "createdAt": "2026-08-18T20:00:00.123456"
  },
  "errorCode": null,
  "message": "계획 조회 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | Plan은 존재하지만 사용자가 `JOINED` 멤버가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 소프트 삭제됨 |

---

#### PATCH `/api/v1/plans/{planId}`

Plan 정보를 부분 수정한다.

`OWNER`, `EDITOR`만 수정 가능하며 `VIEWER`는 수정할 수 없다.

##### Request Body

```json
{
  "title": "제주도 맛집 여행",
  "endDate": "2026-08-19",
  "recruitmentCount": 5
}
```

##### PATCH 필드 처리 규칙

| 요청 | 처리 |
| --- | --- |
| 필드 생략 | 기존 값 유지 |
| 필드에 `null` 전달 | 현재 구현에서는 기존 값 유지 |
| `title` 전달 | 새 제목으로 변경 |
| `description` 전달 | 새 설명으로 변경 |
| `startDate` 전달 | 새 시작일로 변경 |
| `endDate` 전달 | 새 종료일로 변경 |
| `recruitmentCount` 전달 | 새 모집 인원으로 변경 |

수정 요청 결과의 전체 값을 기준으로 다시 날짜 및 모집 인원 검증을 수행한다.

예를 들어 기존 `startDate=2026-08-15`인 Plan에서 `endDate=2026-08-14`만 전달해도 최종 기간이 잘못되므로 수정에 실패한다.

##### Response Body

```json
{
  "success": true,
  "data": {
    "planId": 1,
    "title": "제주도 맛집 여행",
    "description": "여름 제주 여행",
    "startDate": "2026-08-15",
    "endDate": "2026-08-19",
    "recruitmentCount": 5,
    "ownerId": 10,
    "myRole": "EDITOR",
    "memberCount": 4,
    "createdAt": "2026-08-18T20:00:00.123456"
  },
  "errorCode": null,
  "message": "계획 수정 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_INPUT_VALUE` | 제목·설명 길이 또는 모집 인원 DTO 검증 실패 |
| 400 | `INVALID_PLAN_DATE` | 수정 결과 시작일이 종료일보다 늦음 |
| 400 | `INVALID_PLAN_TITLE` | 서비스 계층에서 제목 검증 실패 |
| 400 | `INVALID_RECRUITMENT_COUNT` | 서비스 계층에서 모집 인원 검증 실패 |
| 403 | `PLAN_ACCESS_DENIED` | `JOINED` 멤버가 아님 |
| 403 | `PLAN_UPDATE_FORBIDDEN` | `VIEWER`가 수정 요청 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |

---

#### DELETE `/api/v1/plans/{planId}`

Plan을 소프트 삭제한다.

`OWNER`만 삭제할 수 있다.

##### 처리 순서

```text
1. 삭제되지 않은 Plan을 비관적 쓰기 잠금으로 조회
2. 요청 사용자가 JOINED 멤버인지 확인
3. OWNER인지 확인
4. 해당 Plan의 ACTIVE Invitation을 모두 REVOKED 처리
5. Plan.deletedAt에 현재 시각 설정
```

Member 행의 상태는 변경하지 않는다. Plan 자체가 삭제되어 일반 Plan 조회에서 제외된다.

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": null,
  "errorCode": null,
  "message": "계획 삭제 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | Plan 참여자가 아님 |
| 403 | `MEMBER_ACCESS_DENIED` | 참여 중이지만 OWNER가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 이미 삭제됨 |

> `PLAN_DELETE_FORBIDDEN` 오류 코드는 정의되어 있지만 현재 서비스 구현에서는 사용하지 않는다.

---

### 7.2 Member API

#### GET `/api/v1/plans/{planId}/members`

특정 Plan의 현재 참여 멤버 목록을 조회한다.

해당 Plan에 `JOINED` 상태로 참여 중인 사용자라면 역할과 관계없이 조회할 수 있다.

`LEFT` 멤버는 목록에서 제외한다.

##### Response Body

멤버가 있는 경우:

```json
{
  "success": true,
  "data": [
    {
      "memberId": 1,
      "userId": 10,
      "nickname": "모이고",
      "profileImage": "https://example.com/profile.png",
      "role": "OWNER",
      "status": "JOINED",
      "joinedAt": "2026-08-18T20:00:00.123456"
    },
    {
      "memberId": 2,
      "userId": 11,
      "nickname": "여행자",
      "profileImage": null,
      "role": "EDITOR",
      "status": "JOINED",
      "joinedAt": "2026-08-18T20:30:00.123456"
    }
  ],
  "errorCode": null,
  "message": "멤버 목록 조회 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 요청자가 Plan의 `JOINED` 멤버가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |

---

#### PATCH `/api/v1/plans/{planId}/members/{memberId}/role`

특정 멤버의 역할을 변경한다.

`OWNER`만 실행할 수 있다.

변경 가능한 대상 역할은 실질적으로 `EDITOR`, `VIEWER`다.

##### Request Body

```json
{
  "role": "VIEWER"
}
```

##### 역할 변경 규칙

| 조건 | 처리 |
| --- | --- |
| 요청자가 OWNER | 변경 가능 |
| 요청자가 EDITOR 또는 VIEWER | 변경 불가 |
| 대상이 OWNER | 변경 불가 |
| 새 역할이 `EDITOR` | 변경 가능 |
| 새 역할이 `VIEWER` | 변경 가능 |
| 새 역할이 `OWNER` | 변경 불가 |
| 대상 Member가 다른 Plan 소속 | `MEMBER_NOT_FOUND` |
| 대상 Member가 `LEFT` | `MEMBER_NOT_FOUND` |

##### Response Body

```json
{
  "success": true,
  "data": {
    "memberId": 2,
    "userId": 11,
    "nickname": "여행자",
    "profileImage": null,
    "role": "VIEWER",
    "status": "JOINED",
    "joinedAt": "2026-08-18T20:30:00.123456"
  },
  "errorCode": null,
  "message": "멤버 권한 변경 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 400 | `INVALID_INPUT_VALUE` | `role` 누락 또는 Enum 변환 실패 등 요청 검증 실패 |
| 400 | `INVALID_INVITATION_ROLE` | `OWNER` 권한 부여 시도. 현재 구현에서 사용하는 오류 코드 |
| 403 | `MEMBER_ACCESS_DENIED` | 요청자가 OWNER가 아님 |
| 403 | `OWNER_ROLE_CANNOT_BE_CHANGED` | 대상 멤버가 OWNER |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 404 | `MEMBER_NOT_FOUND` | 대상 Member가 없거나 다른 Plan 소속이거나 `LEFT` 상태 |

---

#### DELETE `/api/v1/plans/{planId}/members/{memberId}`

OWNER가 특정 멤버를 Plan에서 내보낸다.

실제 Member 행을 삭제하지 않고 `status`를 `LEFT`로 변경한다.

##### 처리 규칙

```text
JOINED EDITOR/VIEWER
→ status = LEFT

OWNER
→ 내보내기 불가
```

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": null,
  "errorCode": null,
  "message": "멤버 내보내기 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `MEMBER_ACCESS_DENIED` | 요청자가 OWNER가 아님 |
| 403 | `OWNER_CANNOT_BE_REMOVED` | OWNER를 내보내려 함 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 404 | `MEMBER_NOT_FOUND` | 대상이 없거나 다른 Plan 소속이거나 이미 `LEFT` 상태 |

---

#### DELETE `/api/v1/plans/{planId}/members/me`

현재 로그인한 멤버가 스스로 Plan에서 나간다.

`EDITOR`, `VIEWER`는 나갈 수 있고 `OWNER`는 나갈 수 없다.

##### 처리 결과

```text
status = JOINED
→ status = LEFT
```

Member 행은 유지된다.

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": null,
  "errorCode": null,
  "message": "계획 나가기 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 요청자가 Plan의 `JOINED` 멤버가 아님 |
| 403 | `OWNER_CANNOT_LEAVE` | OWNER가 직접 나가기를 시도 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |

---

### 7.3 Invitation API

#### POST `/api/v1/plans/{planId}/invitations`

Plan의 초대 링크를 발급한다.

`OWNER`만 호출할 수 있다.

이미 사용 가능한 `ACTIVE` 초대 링크가 있다면 새 링크를 만들지 않고 기존 링크를 반환한다.

기존 `ACTIVE` 링크의 `expiresAt`이 이미 지났다면 해당 링크를 `EXPIRED`로 변경하고 새 링크를 생성한다.

##### 초대 코드 생성 규칙

```text
길이: 8자
문자 집합: ABCDEFGHJKLMNPQRSTUVWXYZ23456789
유효기간: 생성 시점 + 7일
```

혼동하기 쉬운 문자 일부를 제외한 영문 대문자와 숫자를 사용한다.

초대 코드 중복이 발생하면 최대 5회까지 새 코드를 생성해 저장을 재시도한다.

##### Request Body

없음.

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": {
    "invitationId": 15,
    "inviteCode": "A7K9PQ2X",
    "status": "ACTIVE",
    "expiresAt": "2026-08-25T20:00:00.123456"
  },
  "errorCode": null,
  "message": "초대 링크 생성 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 요청자가 Plan의 `JOINED` 멤버가 아님 |
| 403 | `MEMBER_ACCESS_DENIED` | 참여 중이지만 OWNER가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 500 | `INVITATION_CODE_DUPLICATED` | 최대 재시도 후에도 고유 코드 저장 실패 |

---

#### POST `/api/v1/plans/{planId}/invitations/reissue`

기존 초대 링크를 무효화하고 새 초대 링크를 발급한다.

`OWNER`만 호출할 수 있다.

##### 처리 순서

```text
1. Plan 존재 여부 확인
2. 요청자가 JOINED 멤버인지 확인
3. OWNER 권한 확인
4. 해당 Plan의 ACTIVE Invitation을 모두 REVOKED 처리
5. 새 8자리 초대 코드 생성
6. expiresAt = 현재 시각 + 7일
7. 새 Invitation 저장 및 반환
```

##### Response Body

```json
{
  "success": true,
  "data": {
    "invitationId": 16,
    "inviteCode": "Z8M4CN7R",
    "status": "ACTIVE",
    "expiresAt": "2026-08-25T21:00:00.123456"
  },
  "errorCode": null,
  "message": "초대 링크 재발급 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 요청자가 Plan의 `JOINED` 멤버가 아님 |
| 403 | `MEMBER_ACCESS_DENIED` | 요청자가 OWNER가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 500 | `INVITATION_CODE_DUPLICATED` | 고유 코드 생성 실패 |

---

#### DELETE `/api/v1/plans/{planId}/invitations/{invitationId}`

특정 초대 링크를 취소한다.

`OWNER`만 호출할 수 있다.

Invitation 행은 삭제하지 않고 `status=REVOKED`로 변경한다.

다른 Plan의 `invitationId`를 전달한 경우 존재 여부를 노출하지 않고 `INVITATION_NOT_FOUND`로 처리한다.

이미 `REVOKED` 또는 `EXPIRED` 상태인 Invitation에 다시 취소 요청을 보내도 현재 구현은 `REVOKED`로 설정하고 정상 종료한다.

##### Response Body

성공 상태: `200 OK`

```json
{
  "success": true,
  "data": null,
  "errorCode": null,
  "message": "초대 링크 취소 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 403 | `PLAN_ACCESS_DENIED` | 요청자가 Plan의 `JOINED` 멤버가 아님 |
| 403 | `MEMBER_ACCESS_DENIED` | 요청자가 OWNER가 아님 |
| 404 | `PLAN_NOT_FOUND` | Plan이 없거나 삭제됨 |
| 404 | `INVITATION_NOT_FOUND` | Invitation이 없거나 다른 Plan 소속 |

---

#### GET `/api/v1/invitations/{inviteCode}`

초대 코드 정보를 조회한다.

현재 Security 설정 기준으로 인증된 사용자만 호출할 수 있다.

조회 시 `ACTIVE` 상태이지만 `expiresAt`이 지난 링크는 즉시 `EXPIRED`로 상태를 변경한 뒤 반환한다.

취소되거나 만료된 링크도 조회 자체는 가능하며 현재 상태를 응답한다.

##### Response Body

활성 링크:

```json
{
  "success": true,
  "data": {
    "invitationId": 15,
    "inviteCode": "A7K9PQ2X",
    "status": "ACTIVE",
    "expiresAt": "2026-08-25T20:00:00.123456"
  },
  "errorCode": null,
  "message": "초대 정보 조회 성공"
}
```

만료 링크:

```json
{
  "success": true,
  "data": {
    "invitationId": 10,
    "inviteCode": "BC7QM2KP",
    "status": "EXPIRED",
    "expiresAt": "2026-08-10T20:00:00.123456"
  },
  "errorCode": null,
  "message": "초대 정보 조회 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증되지 않은 사용자 |
| 404 | `INVITATION_NOT_FOUND` | 해당 초대 코드가 없음 |

---

#### POST `/api/v1/invitations/{inviteCode}/join`

초대 코드를 사용해 해당 Plan에 참여한다.

신규 참여자와 재참여자 모두 `EDITOR` 권한으로 참여한다.

Invitation은 참여 후에도 소모되지 않으며, `ACTIVE`이고 만료되지 않은 동안 다른 사용자도 같은 코드로 참여할 수 있다.

##### 검증 순서

```text
1. 인증 여부 확인
2. inviteCode 존재 여부 확인
3. 만료 시각 확인 및 필요 시 EXPIRED 상태 갱신
4. REVOKED 여부 확인
5. EXPIRED 여부 확인
6. 삭제되지 않은 Plan을 비관적 쓰기 잠금으로 조회
7. 사용자 조회
8. 동일 Plan의 기존 Member 참여 이력 조회
9. 이미 JOINED 상태이면 중복 참여 오류
10. recruitmentCount가 있으면 현재 JOINED 인원 확인
11. 정원이 찼으면 참여 거절
12. LEFT 이력이 있으면 기존 Member를 EDITOR/JOINED로 재활성화
13. 이력이 없으면 새 EDITOR/JOINED Member 생성
14. 참여 결과 반환
```

Plan을 비관적 쓰기 잠금으로 조회한 뒤 정원을 검사하여 동시에 여러 사용자가 마지막 자리에 참여하는 상황을 줄인다.

##### 신규 참여

```text
Member 없음
→ 새 Member 생성
→ role = EDITOR
→ status = JOINED
→ joinedAt = 현재 시각
```

##### 재참여

```text
기존 Member.status = LEFT
→ 같은 memberId 재사용
→ role = EDITOR
→ status = JOINED
→ joinedAt = 현재 시각으로 갱신
```

이전 역할이 `VIEWER`였더라도 재참여 시 `EDITOR`로 변경된다.

##### Response Body

```json
{
  "success": true,
  "data": {
    "planId": 1,
    "memberId": 25,
    "role": "EDITOR"
  },
  "errorCode": null,
  "message": "계획 참여 성공"
}
```

##### Error Code

| 상태 | 오류 코드 | 조건 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증 사용자 없음 또는 사용자 조회 실패 |
| 404 | `INVITATION_NOT_FOUND` | 초대 코드가 없음 |
| 404 | `PLAN_NOT_FOUND` | 연결된 Plan이 없거나 삭제됨 |
| 409 | `INVITATION_EXPIRED` | 초대 링크가 만료됨 |
| 409 | `INVITATION_REVOKED` | 초대 링크가 취소됨 |
| 409 | `MEMBER_ALREADY_JOINED` | 이미 해당 Plan에 `JOINED` 상태로 참여 중 |
| 409 | `PLAN_RECRUITMENT_FULL` | 모집 인원이 가득 참 |

---

## 8. 테스트 시나리오

### Plan

- 유효한 요청으로 Plan을 생성하면 생성자가 `OWNER` 멤버로 함께 등록된다.
- 설명 또는 모집 인원을 생략해도 Plan을 생성할 수 있으며, 모집 인원이 없으면 무제한으로 처리한다.
- 제목·날짜·모집 인원 등 필수 입력값이 유효하지 않으면 요청을 거부한다.
- 참여 중인 Plan 목록에서는 `LEFT` 상태의 멤버와 삭제된 Plan을 제외한다.
- Plan 상세 조회 시 현재 역할과 참여 인원 수를 함께 확인할 수 있다.
- `OWNER`, `EDITOR`는 Plan을 수정할 수 있고 `VIEWER`는 수정할 수 없다.
- Plan 삭제는 `OWNER`만 가능하며, 삭제 시 활성 초대 링크를 함께 취소한다.

### Member

- Plan 참여자는 현재 `JOINED` 상태의 멤버 목록을 조회할 수 있다.
- `OWNER`는 `EDITOR`와 `VIEWER`의 역할을 변경할 수 있다.
- `OWNER` 자신의 역할은 변경하거나 다른 멤버처럼 내보낼 수 없다.
- `OWNER`는 다른 `EDITOR`, `VIEWER` 멤버를 내보낼 수 있으며 대상 멤버는 `LEFT` 상태가 된다.
- `EDITOR`, `VIEWER`는 스스로 Plan에서 나갈 수 있고 `OWNER`는 직접 나갈 수 없다.
- `LEFT` 상태의 멤버는 일반 멤버 조회 및 권한 변경 대상에서 제외한다.

### Invitation

- `OWNER`가 초대 링크를 발급하면 유효한 `ACTIVE` 초대 코드가 생성된다.
- 이미 사용 가능한 `ACTIVE` 링크가 있으면 기존 링크를 반환하고, 재발급 시 기존 링크를 취소한 뒤 새 링크를 생성한다.
- 만료된 초대 링크는 `EXPIRED`, 취소된 링크는 `REVOKED` 상태로 처리한다.
- 유효한 초대 링크로 참여하면 신규 멤버는 `EDITOR/JOINED` 상태로 등록된다.
- 이전에 나간 멤버가 다시 참여하면 기존 Member 행을 재사용하고 `EDITOR/JOINED` 상태로 변경한다.
- 이미 참여 중인 사용자, 모집 정원을 초과한 사용자, 만료·취소된 링크를 사용한 사용자는 참여할 수 없다.
- 동일 초대 링크는 만료·취소되기 전까지 모집 정원 범위에서 여러 사용자가 사용할 수 있다.

---

## 9. 상태 전이

### Plan

```text
ACTIVE
  |
  | OWNER DELETE
  v
DELETED (deletedAt != null)
```

삭제된 Plan은 일반 조회·수정·멤버 관리·초대 발급 대상에서 제외한다.

### Member

```text
               leave / remove
JOINED  ----------------------------> LEFT
  ^                                      |
  |                                      |
  |             invitation join          |
  +--------------------------------------+
```

재참여 시 동일 `(planId, userId)` Member 행을 사용한다.

OWNER는 `LEFT` 상태로 전환할 수 없다.

### Invitation

```text
                 expiresAt 경과
ACTIVE  ------------------------------> EXPIRED
  |
  | revoke / reissue / plan delete
  v
REVOKED
```

`EXPIRED`, `REVOKED` 상태는 다시 `ACTIVE`로 복구하지 않는다. 새 링크가 필요하면 재발급한다.

---
