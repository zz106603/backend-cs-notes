package com.csnotes.rag.indexing;

import com.csnotes.document.DocumentService;
import com.csnotes.rag.chunk.HeadingAwareMarkdownChunker;
import com.csnotes.rag.chunk.MarkdownChunker;
import com.csnotes.rag.embedding.EmbeddingProvider;
import com.csnotes.rag.embedding.OpenAiApiKeyCondition;
import com.csnotes.rag.persistence.ChunkVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(OpenAiApiKeyCondition.class)
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.indexing.enabled"}, havingValue = "true")
public class RagIndexingConfiguration {
    @Bean
    MarkdownChunker markdownChunker(@Value("${rag.indexing.chunk-max-characters}") int maxCharacters) {
        return new HeadingAwareMarkdownChunker(maxCharacters);
    }

    @Bean
    RagIndexingService ragIndexingService(
            DocumentService documentService,
            MarkdownChunker chunker,
            EmbeddingProvider embeddingProvider,
            ChunkVectorStore vectorStore,
            @Value("${rag.indexing.max-documents}") int maxDocuments,
            @Value("${rag.indexing.max-chunks-per-document}") int maxChunksPerDocument,
            @Value("${rag.indexing.max-characters-per-run}") long maxCharactersPerRun
    ) {
        return new RagIndexingService(documentService, chunker, embeddingProvider, vectorStore,
                maxDocuments, maxChunksPerDocument, maxCharactersPerRun);
    }
}
