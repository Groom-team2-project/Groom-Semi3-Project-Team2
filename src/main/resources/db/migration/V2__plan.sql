-- members: 나가기/재참여 기능에 필요
ALTER TABLE members ADD COLUMN status ENUM('JOINED', 'LEFT') NOT NULL DEFAULT 'JOINED';
ALTER TABLE members ADD COLUMN joined_at DATETIME(6) NULL;

-- invitations: 발급자 기록, 취소/재발급 처리에 필요
ALTER TABLE invitations ADD COLUMN inviter_id BIGINT NULL;
ALTER TABLE invitations ADD COLUMN status ENUM('ACTIVE', 'EXPIRED', 'REVOKED') NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE invitations ADD CONSTRAINT fk_invitations_inviter
    FOREIGN KEY (inviter_id) REFERENCES users (user_id);

-- plans: 소프트 삭제
ALTER TABLE plans ADD COLUMN deleted_at DATETIME(6) NULL;

-- 기존 members row에 joined_at 채우기
UPDATE members
SET joined_at = (
    SELECT p.created_at FROM plans p WHERE p.plan_id = members.plan_id
)
WHERE joined_at IS NULL;