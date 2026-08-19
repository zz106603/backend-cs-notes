package com.csnotes.rag.persistence;

import com.csnotes.rag.chunk.DocumentChunk;
import com.csnotes.rag.embedding.EmbeddingVector;

public record EmbeddedChunk(DocumentChunk chunk, EmbeddingVector embedding) {
    public EmbeddedChunk {
        if (!chunk.id().equals(embedding.inputId())) {
            throw new IllegalArgumentException("Chunk ID and embedding input ID must match");
        }
    }
}
