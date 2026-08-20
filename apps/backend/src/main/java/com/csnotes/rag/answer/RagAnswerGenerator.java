package com.csnotes.rag.answer;

interface RagAnswerGenerator {
    String modelName();

    GeneratedAnswer generate(String question, String groundedContext);
}
