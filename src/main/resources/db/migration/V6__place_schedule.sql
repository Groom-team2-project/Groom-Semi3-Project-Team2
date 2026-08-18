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
SET start_at = TIMESTAMP(
    schedule_date,
    COALESCE(start_time, '00:00:00')
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


-- schedule_date가 포함된 기존 인덱스를 먼저 제거한다.
DROP INDEX idx_schedules_plan_date ON schedules;


-- 더 이상 사용하지 않는 기존 컬럼 제거
ALTER TABLE schedules
DROP COLUMN schedule_date,
    DROP COLUMN start_time,
    DROP COLUMN is_reserved;


-- 활성 일정 목록과 정렬 순서 조회를 위한 인덱스
CREATE INDEX idx_schedules_plan_deleted_sort
    ON schedules (plan_id, deleted_at, sort_order);