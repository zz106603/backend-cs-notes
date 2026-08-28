package com.csnotes.rag.evaluation;

import com.csnotes.rag.search.RagSearchValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RagEvaluationController.class)
public class RagEvaluationExceptionHandler {
    @ExceptionHandler(RagEvaluationValidationException.class)
    ProblemDetail invalidEvaluation(RagEvaluationValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("검색 평가 요청이 올바르지 않습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(RagSearchValidationException.class)
    ProblemDetail unavailableSearch(RagSearchValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("검색 평가를 실행할 수 없습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
