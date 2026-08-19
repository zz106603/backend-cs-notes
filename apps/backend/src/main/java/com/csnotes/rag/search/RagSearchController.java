package com.csnotes.rag.search;

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
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled"}, havingValue = "true")
public class RagSearchController {
    private final RagSearchService searchService;

    public RagSearchController(RagSearchService searchService) {
        this.searchService = searchService;
    }

    /** 명시적인 POST 요청만 유료 질의 임베딩을 호출한다. */
    @PostMapping("/search")
    public RagSearchResponse search(@RequestBody RagSearchRequest request) {
        return searchService.search(request);
    }
}
