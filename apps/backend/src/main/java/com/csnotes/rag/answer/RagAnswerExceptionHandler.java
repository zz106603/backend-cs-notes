package com.csnotes.rag.answer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RagAnswerController.class)
public class RagAnswerExceptionHandler {
    @ExceptionHandler(RagAnswerValidationException.class)
    ProblemDetail invalidAnswerRequest(RagAnswerValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("RAG 답변 요청이 올바르지 않습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(RagAnswerLimitExceededException.class)
    ProblemDetail answerCostLimit(RagAnswerLimitExceededException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        detail.setTitle("RAG 답변 비용 한도에 도달했습니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(RagAnswerInProgressException.class)
    ProblemDetail duplicateAnswer(RagAnswerInProgressException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("동일한 답변을 생성 중입니다.");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail answerFailure(Exception exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detail.setTitle("RAG 답변을 생성하지 못했습니다.");
        detail.setDetail("OpenAI 응답 또는 사용량 저장 상태를 확인한 뒤 다시 시도해 주세요.");
        return detail;
    }
}
