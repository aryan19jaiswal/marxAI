CREATE TABLE progress (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic         VARCHAR(100) NOT NULL,
    subtopic      VARCHAR(100),
    score         INT NOT NULL DEFAULT 0,
    status        VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    attempts      INT NOT NULL DEFAULT 0,
    last_practiced TIMESTAMP
);

CREATE INDEX idx_progress_user_id ON progress (user_id);

CREATE TABLE study_plans (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    plan_json  JSONB NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_study_plans_user_id ON study_plans (user_id);
