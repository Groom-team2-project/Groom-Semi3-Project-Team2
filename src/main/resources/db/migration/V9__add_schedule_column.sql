ALTER TABLE schedules
    ADD COLUMN end_at DATETIME(6) NULL AFTER start_at;

UPDATE schedules
SET end_at = CAST(
        CONCAT(
                CAST(start_at AS DATE),
                ' ',
                end_time
        )
    AS DATETIME
             )
WHERE end_time IS NOT NULL;

ALTER TABLE schedules
DROP COLUMN end_time;