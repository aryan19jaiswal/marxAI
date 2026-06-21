CREATE TABLE resume (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    s3_key      VARCHAR(512) NOT NULL,
    parsed_data JSONB,
    ats_score   INT,
    feedback    JSONB,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_resume_user_id ON resume (user_id);
