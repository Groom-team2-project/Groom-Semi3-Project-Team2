-- =========================================================
-- Place: PlaceEntity와 DB 스키마 일치
-- =========================================================

ALTER TABLE places
    RENAME COLUMN place_name TO name;

ALTER TABLE places
    RENAME COLUMN image_url TO place_url;

ALTER TABLE places
    ADD COLUMN road_address VARCHAR(300) NULL AFTER address;

ALTER TABLE places
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;

ALTER TABLE places
    MODIFY COLUMN category VARCHAR(255) NULL;


-- =========================================================
-- Schedule: 기존 데이터를 보존하면서 신규 구조로 변경
-- =========================================================

-- 먼저 nullable로 추가해서 기존 행이 있어도 마이그레이션되게 한다.
ALTER TABLE schedules
    ADD COLUMN start_at DATETIME(6) NULL AFTER title;

ALTER TABLE schedules
    ADD COLUMN reservation_status ENUM(
        'NOT_REQUIRED',
        'UNRESERVED',
        'RESERVED',
        'CANCELLED'
    ) NULL AFTER memo;

ALTER TABLE schedules
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;


-- 기존 날짜와 시간을 start_at으로 합친다.
-- start_time이 없던 일정은 해당 날짜의 00:00:00으로 변환한다.
UPDATE schedules
SET start_at = CAST(
    CONCAT(schedule_date, ' ', COALESCE(start_time, '00:00:00'))
    AS DATETIME
    );


-- 기존 예약 여부를 신규 예약 상태로 변환한다.
UPDATE schedules
SET reservation_status = CASE
                             WHEN is_reserved = TRUE THEN 'RESERVED'
                             ELSE 'NOT_REQUIRED'
    END;


-- 데이터 변환 후 신규 필드의 NOT NULL 제약을 적용한다.
ALTER TABLE schedules
    MODIFY COLUMN start_at DATETIME(6) NOT NULL;

ALTER TABLE schedules
    MODIFY COLUMN reservation_status ENUM(
    'NOT_REQUIRED',
    'UNRESERVED',
    'RESERVED',
    'CANCELLED'
    ) NOT NULL DEFAULT 'NOT_REQUIRED';

-- 신규 인덱스를 먼저 생성한다.
-- plan_id가 첫 번째 컬럼이므로 기존 외래 키 인덱스 역할도 대신한다.
CREATE INDEX idx_schedules_plan_deleted_sort
    ON schedules (plan_id, deleted_at, sort_order);

-- 대체 인덱스를 만든 다음 기존 인덱스를 제거한다.
DROP INDEX idx_schedules_plan_date ON schedules;


-- 더 이상 사용하지 않는 기존 컬럼 제거
ALTER TABLE schedules
    DROP COLUMN schedule_date;

ALTER TABLE schedules
    DROP COLUMN start_time;

ALTER TABLE schedules
    DROP COLUMN is_reserved;
