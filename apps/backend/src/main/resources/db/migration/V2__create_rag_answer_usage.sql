CREATE TABLE rag_answer_usage (
    request_id          uuid PRIMARY KEY,
    question_hash       varchar(64) NOT NULL,
    model               varchar(100) NOT NULL,
    status              varchar(30) NOT NULL,
    prompt_tokens       integer,
    completion_tokens   integer,
    total_tokens        integer,
    estimated_cost_usd  numeric(12, 8) NOT NULL DEFAULT 0,
    source_count        integer NOT NULL DEFAULT 0,
    context_characters  integer NOT NULL DEFAULT 0,
    elapsed_ms          bigint NOT NULL DEFAULT 0,
    failure_type        varchar(120),
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX rag_answer_usage_created_at_idx ON rag_answer_usage (created_at);

