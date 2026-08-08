CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kakao_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_kakao_id UNIQUE (kakao_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
);

CREATE TABLE plans (
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    recruitment_count INT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_plans_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('OWNER', 'EDITOR', 'VIEWER') NOT NULL,
    CONSTRAINT uk_members_plan_user UNIQUE (plan_id, user_id),
    CONSTRAINT fk_members_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_members_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE invitations (
    invitation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    invite_code VARCHAR(20) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_invitations_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_invitations_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id)
);

CREATE TABLE places (
    place_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kakao_place_id VARCHAR(50) NOT NULL,
    place_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    address VARCHAR(300),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    phone VARCHAR(30),
    image_url VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_places_kakao_place_id UNIQUE (kakao_place_id)
);

CREATE TABLE schedules (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    place_id BIGINT,
    schedule_date DATE NOT NULL,
    kakao_route_url VARCHAR(500),
    title VARCHAR(200) NOT NULL,
    start_time TIME,
    end_time TIME,
    memo VARCHAR(1000),
    sort_order INT NOT NULL,
    is_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_schedules_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_schedules_place
        FOREIGN KEY (place_id) REFERENCES places (place_id)
);

CREATE TABLE comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    parent_comment_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_comments_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_comments_schedule
        FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id),
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comments (comment_id)
);

CREATE TABLE votes (
    vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    type ENUM('SINGLE', 'MULTIPLE') NOT NULL,
    end_datetime DATETIME(6) NOT NULL,
    status ENUM('OPEN', 'CLOSED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_votes_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_votes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE vote_options (
    option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    place_id BIGINT,
    content VARCHAR(200) NOT NULL,
    CONSTRAINT fk_vote_options_vote
        FOREIGN KEY (vote_id) REFERENCES votes (vote_id),
    CONSTRAINT fk_vote_options_place
        FOREIGN KEY (place_id) REFERENCES places (place_id)
);

CREATE TABLE vote_participants (
    participation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_vote_participants_vote_user_option
        UNIQUE (vote_id, user_id, option_id),
    CONSTRAINT fk_vote_participants_vote
        FOREIGN KEY (vote_id) REFERENCES votes (vote_id),
    CONSTRAINT fk_vote_participants_option
        FOREIGN KEY (option_id) REFERENCES vote_options (option_id),
    CONSTRAINT fk_vote_participants_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE activity_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    user_id BIGINT,
    action_type ENUM(
        'SCHEDULE_CREATED',
        'SCHEDULE_UPDATED',
        'SCHEDULE_DELETED',
        'VOTE_CREATED',
        'VOTE_CLOSED',
        'VOTE_PARTICIPATED',
        'MEMBER_JOINED',
        'MEMBER_LEFT',
        'MEMBER_ROLE_CHANGED',
        'COMMENT_CREATED',
        'INVITATION_CREATED',
        'INVITATION_REVOKED'
    ) NOT NULL,
    target_type ENUM('SCHEDULE', 'VOTE', 'MEMBER', 'COMMENT', 'INVITATION') NOT NULL,
    target_id BIGINT NOT NULL,
    summary VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_activity_logs_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_activity_logs_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    INDEX idx_activity_logs_plan_created_at (plan_id, created_at),
    INDEX idx_activity_logs_target (target_type, target_id)
);

CREATE INDEX idx_schedules_plan_date
    ON schedules (plan_id, schedule_date);

CREATE INDEX idx_comments_schedule
    ON comments (schedule_id);

CREATE INDEX idx_vote_participants_vote_user
    ON vote_participants (vote_id, user_id);
