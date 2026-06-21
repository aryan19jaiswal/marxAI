CREATE TABLE documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    filename    VARCHAR(255) NOT NULL,
    s3_key      VARCHAR(512) NOT NULL,
    doc_type    VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    chunk_count INT NOT NULL DEFAULT 0,
    metadata    JSONB,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_user_id ON documents (user_id);

CREATE TABLE chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    qdrant_id   VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    metadata    JSONB
);

CREATE INDEX idx_chunks_document_id ON chunks (document_id);
