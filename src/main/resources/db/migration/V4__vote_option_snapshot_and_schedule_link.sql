-- 투표 선택지에 후보 장소의 주소·이모지 스냅샷을 추가한다.
-- 프론트엔드는 카카오 장소 검색 결과를 place_id 없이 이름·주소·이모지로 보내고,
-- 이모지는 places 테이블에도 둘 자리가 없어 선택지에 직접 저장한다.
-- 투표는 당시 후보가 무엇이었는지 남는 편이 맞기도 하다.
ALTER TABLE vote_options
    ADD COLUMN place_address VARCHAR(300) NULL AFTER content;

ALTER TABLE vote_options
    ADD COLUMN emoji VARCHAR(20) NOT NULL DEFAULT '📍' AFTER place_address;

-- 투표와 일정을 연결한다. "이 일정 자리를 뭘로 채울까" 형태의 투표에 쓰인다.
-- 프론트엔드 타임라인은 일정에서 투표로, 투표 상세는 일정으로 서로 오간다.
ALTER TABLE votes
    ADD COLUMN schedule_id BIGINT NULL AFTER plan_id;

ALTER TABLE votes
    ADD CONSTRAINT fk_votes_schedule
        FOREIGN KEY (schedule_id) REFERENCES schedules (schedule_id);

CREATE INDEX idx_votes_plan_id ON votes (plan_id);

CREATE INDEX idx_votes_schedule_id ON votes (schedule_id);

CREATE INDEX idx_vote_options_vote_id ON vote_options (vote_id);
