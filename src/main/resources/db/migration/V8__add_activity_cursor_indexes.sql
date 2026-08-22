CREATE INDEX idx_activity_logs_plan_created_log
    ON activity_logs (plan_id, created_at DESC, log_id DESC);

CREATE INDEX idx_activity_logs_user_created_log
    ON activity_logs (user_id, created_at DESC, log_id DESC);
