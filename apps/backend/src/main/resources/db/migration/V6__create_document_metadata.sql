CREATE TABLE document (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    source_document_id TEXT NOT NULL,
    file_path TEXT NOT NULL,
    title TEXT NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_document_file_path UNIQUE (file_path),
    CONSTRAINT ck_document_visibility CHECK (visibility IN ('PRIVATE', 'PUBLIC'))
);

CREATE INDEX idx_document_owner_active ON document (owner_id, deleted_at);
CREATE INDEX idx_document_source_id ON document (source_document_id);
