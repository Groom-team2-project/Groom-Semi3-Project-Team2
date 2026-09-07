-- 댓글 목록의 커서 페이지네이션 지원 인덱스
CREATE INDEX idx_comments_schedule_parent_created
    ON comments (schedule_id, parent_comment_id, created_at, comment_id);
