package com.csnotes.rag.answer;

import com.csnotes.rag.search.RagSearchHit;

import java.util.List;

public record RagAnswerSource(
        int number,
        String chunkId,
        String documentId,
        String documentTitle,
        String documentPath,
        List<String> sectionPath,
        double score
) {
    static RagAnswerSource from(int number, RagSearchHit hit) {
        return new RagAnswerSource(number, hit.chunkId(), hit.documentId(), hit.documentTitle(),
                hit.documentPath(), hit.sectionPath(), hit.score());
    }
}
