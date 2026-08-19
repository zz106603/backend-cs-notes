package com.csnotes.rag.indexing;

import com.csnotes.rag.embedding.OpenAiApiKeyCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@Conditional(OpenAiApiKeyCondition.class)
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.indexing.enabled"}, havingValue = "true")
public class RagIndexingController {
    private final RagIndexingService indexingService;

    public RagIndexingController(RagIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /** 명시적으로 dryRun=false를 보낸 경우에만 OpenAI 호출과 DB 변경을 수행한다. */
    @PostMapping("/index")
    public RagIndexingResult index(@RequestParam(defaultValue = "true") boolean dryRun) {
        return indexingService.synchronize(dryRun);
    }
}
