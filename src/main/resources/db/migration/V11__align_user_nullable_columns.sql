UPDATE users
SET nickname = CONCAT('사용자', user_id)
WHERE nickname IS NULL OR TRIM(nickname) = '';

ALTER TABLE users
    MODIFY COLUMN email VARCHAR(255) NULL;

ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(50) NOT NULL;
