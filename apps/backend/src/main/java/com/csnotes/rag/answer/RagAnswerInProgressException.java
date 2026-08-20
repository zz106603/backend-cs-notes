package com.csnotes.rag.answer;

public class RagAnswerInProgressException extends RuntimeException {
    RagAnswerInProgressException() {
        super("같은 질문의 답변을 이미 생성하고 있습니다. 잠시 후 다시 시도해 주세요.");
    }
}

