package com.csnotes.rag.answer;

import com.csnotes.rag.search.RagSearchHit;
import com.csnotes.rag.search.RagSearchMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagAnswerSourceTest {
    @Test
    void Reranker가_적용되면_출처에_최종_관련성_점수를_사용한다() {
        RagSearchHit hit = new RagSearchHit("chunk-1", "doc-1", "문서", "문서.md",
                List.of(), List.of("섹션"), "본문", 0.31,
                0.7, 0.4, 2, 3, 0.92, 1, List.of(RagSearchMode.DENSE, RagSearchMode.SPARSE));

        RagAnswerSource source = RagAnswerSource.from(1, hit);

        assertThat(source.score()).isEqualTo(0.92);
    }
}
