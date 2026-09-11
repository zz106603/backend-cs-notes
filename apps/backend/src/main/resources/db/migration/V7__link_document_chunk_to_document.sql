ALTER TABLE document_chunk
    ADD COLUMN document_metadata_id UUID;

UPDATE document_chunk chunk
   SET document_metadata_id = document.id
  FROM document
 WHERE document.deleted_at IS NULL
   AND (document.source_document_id = chunk.document_id OR document.file_path = chunk.document_path);

ALTER TABLE document_chunk
    ADD CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_metadata_id) REFERENCES document(id) ON DELETE CASCADE;

CREATE INDEX idx_document_chunk_metadata_id ON document_chunk (document_metadata_id);
