package com.csnotes.rag.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class HeadingAwareMarkdownChunkerTest {
    private final HeadingAwareMarkdownChunker chunker = new HeadingAwareMarkdownChunker(240);

    @Test
    void 제목_계층을_청크_메타데이터로_보존한다() {
        var document = document("""
                # Spring

                개요입니다.

                ## 트랜잭션

                전파 속성을 설명합니다.

                ### REQUIRES_NEW

                별도 트랜잭션을 생성합니다.
                """);

        var chunks = chunker.chunk(document);

        assertThat(chunks).extracting(DocumentChunk::sectionPath)
                .containsExactly(List.of("Spring"), List.of("Spring", "트랜잭션"),
                        List.of("Spring", "트랜잭션", "REQUIRES_NEW"));
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.documentId()).isEqualTo("doc-1");
            assertThat(chunk.tags()).containsExactly("Spring", "트랜잭션");
        });
    }

    @Test
    void 목표_크기를_초과해도_코드_블록을_하나로_유지한다() {
        String code = "x".repeat(300);
        var chunks = chunker.chunk(document("# 코드\n\n```java\n" + code + "\n```\n\n설명입니다."));

        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk.content())
                .contains("```java").contains(code).contains("```") );
    }

    @Test
    void 긴_본문을_분할하고_안정적인_아이디와_해시를_생성한다() {
        var document = document("# 긴 문서\n\n" + "긴 내용을 설명합니다. ".repeat(80));

        var first = chunker.chunk(document);
        var second = chunker.chunk(document);

        assertThat(first).hasSizeGreaterThan(1);
        assertThat(first).extracting(DocumentChunk::id).containsExactlyElementsOf(second.stream().map(DocumentChunk::id).toList());
        assertThat(first).allSatisfy(chunk -> assertThat(chunk.contentHash()).hasSize(64));
        assertThat(first).extracting(DocumentChunk::sequence)
                .containsExactlyElementsOf(IntStream.range(0, first.size()).boxed().toList());
    }

    private ChunkSourceDocument document(String content) {
        return new ChunkSourceDocument("doc-1", "테스트", "백엔드/테스트.md",
                List.of("Spring", "트랜잭션"), content);
    }
}
