ALTER TABLE schedules
    ADD COLUMN end_at DATETIME(6) NULL AFTER start_at;

UPDATE schedules
SET end_at = TIMESTAMP(DATE(start_at), end_time)
WHERE end_time IS NOT NULL;

ALTER TABLE schedules
DROP COLUMN end_time;