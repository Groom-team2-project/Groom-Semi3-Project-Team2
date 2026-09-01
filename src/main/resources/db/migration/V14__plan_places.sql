CREATE TABLE plan_places (
    plan_place_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_plan_places_plan_place UNIQUE (plan_id, place_id),
    CONSTRAINT fk_plan_places_plan
        FOREIGN KEY (plan_id) REFERENCES plans (plan_id),
    CONSTRAINT fk_plan_places_place
        FOREIGN KEY (place_id) REFERENCES places (place_id)
);

CREATE INDEX idx_plan_places_plan ON plan_places (plan_id);
