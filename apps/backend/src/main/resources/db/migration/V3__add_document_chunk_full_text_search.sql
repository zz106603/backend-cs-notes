ALTER TABLE document_chunk ADD COLUMN search_vector tsvector;

CREATE FUNCTION document_chunk_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.document_title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.tags::text, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.section_path::text, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.document_path, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.content, '')), 'D');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER document_chunk_search_vector_trigger
    BEFORE INSERT OR UPDATE OF document_title, document_path, tags, section_path, content
    ON document_chunk
    FOR EACH ROW EXECUTE FUNCTION document_chunk_search_vector_update();

UPDATE document_chunk
SET search_vector =
    setweight(to_tsvector('simple', coalesce(document_title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(tags::text, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(section_path::text, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(document_path, '')), 'B') ||
    setweight(to_tsvector('simple', coalesce(content, '')), 'D');

ALTER TABLE document_chunk ALTER COLUMN search_vector SET NOT NULL;

CREATE INDEX document_chunk_search_vector_gin_idx
    ON document_chunk USING gin (search_vector);
