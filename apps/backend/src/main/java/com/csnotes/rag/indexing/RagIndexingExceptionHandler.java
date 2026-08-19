package com.csnotes.rag.indexing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RagIndexingController.class)
public class RagIndexingExceptionHandler {
    @ExceptionHandler(RagIndexingLimitException.class)
    ProblemDetail indexingLimit(RagIndexingLimitException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        detail.setTitle("RAG 색인 비용 한도를 초과했습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail indexingConflict(IllegalStateException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("RAG 색인을 실행할 수 없습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
