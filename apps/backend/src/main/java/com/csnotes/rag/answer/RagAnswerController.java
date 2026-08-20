package com.csnotes.rag.answer;

import com.csnotes.rag.embedding.OpenAiApiKeyCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@Conditional(OpenAiApiKeyCondition.class)
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled", "rag.answer.enabled"}, havingValue = "true")
public class RagAnswerController {
    private final RagAnswerService answerService;

    public RagAnswerController(RagAnswerService answerService) {
        this.answerService = answerService;
    }

    /** 검색과 답변 생성이라는 유료 호출은 명시적인 POST 요청에서만 수행한다. */
    @PostMapping("/answer")
    public RagAnswerResponse answer(@RequestBody RagAnswerRequest request) {
        return answerService.answer(request);
    }
}
