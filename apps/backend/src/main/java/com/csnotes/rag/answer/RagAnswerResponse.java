package com.csnotes.rag.answer;

import java.util.List;

public record RagAnswerResponse(
        String question,
        String answer,
        String answerModel,
        boolean generated,
        boolean cached,
        int contextCharacters,
        RagAnswerUsage usage,
        List<RagAnswerSource> sources
) {
}
