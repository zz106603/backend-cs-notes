package com.csnotes.rag.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled"}, havingValue = "true")
public class RagSearchController {
    private final RagSearchService searchService;

    public RagSearchController(RagSearchService searchService) {
        this.searchService = searchService;
    }

    /** Dense는 질의 임베딩을 호출하고 Sparse는 PostgreSQL FTS만 사용한다. */
    @PostMapping("/search")
    public RagSearchResponse search(@RequestBody RagSearchRequest request) {
        return searchService.search(request);
    }
}
