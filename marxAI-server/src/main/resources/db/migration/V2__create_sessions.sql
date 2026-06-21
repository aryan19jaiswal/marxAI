CREATE TABLE sessions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    agent_type VARCHAR(50) NOT NULL,
    metadata   JSONB,
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    ended_at   TIMESTAMP
);

CREATE INDEX idx_sessions_user_id ON sessions (user_id);

CREATE TABLE conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    tool_calls  JSONB,
    tokens_used INT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_session_id ON conversations (session_id);
