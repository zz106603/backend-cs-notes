CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunk (
    id              varchar(64) PRIMARY KEY,
    document_id     text NOT NULL,
    document_title  text NOT NULL,
    document_path   text NOT NULL,
    tags            jsonb NOT NULL DEFAULT '[]'::jsonb,
    section_path    jsonb NOT NULL DEFAULT '[]'::jsonb,
    sequence        integer NOT NULL,
    content         text NOT NULL,
    content_hash    varchar(64) NOT NULL,
    embedding_model text NOT NULL,
    embedding       vector(1536) NOT NULL,
    indexed_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (document_id, sequence)
);

CREATE INDEX document_chunk_document_id_idx ON document_chunk (document_id);
CREATE INDEX document_chunk_embedding_hnsw_idx
    ON document_chunk USING hnsw (embedding vector_cosine_ops);
