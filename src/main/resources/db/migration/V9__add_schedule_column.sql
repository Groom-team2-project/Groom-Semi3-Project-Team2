ALTER TABLE schedules
    ADD COLUMN end_at DATETIME(6) NULL AFTER start_at;

ALTER TABLE schedules
DROP COLUMN end_time;

SELECT EXISTS (
    SELECT 1
    FROM schedules
    WHERE plan_id = ?
      AND sort_order = ?
      AND deleted_at IS NULL
);