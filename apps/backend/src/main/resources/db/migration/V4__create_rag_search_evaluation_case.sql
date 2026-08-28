CREATE TABLE rag_search_evaluation_case (
    id                       uuid PRIMARY KEY,
    query                    varchar(500) NOT NULL,
    expected_document_paths  text NOT NULL,
    created_at               timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX rag_search_evaluation_case_created_at_idx
    ON rag_search_evaluation_case (created_at DESC);
