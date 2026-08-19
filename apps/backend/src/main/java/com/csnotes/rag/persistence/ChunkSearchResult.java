package com.csnotes.rag.persistence;

import com.csnotes.rag.chunk.DocumentChunk;

public record ChunkSearchResult(DocumentChunk chunk, double score) {
}
