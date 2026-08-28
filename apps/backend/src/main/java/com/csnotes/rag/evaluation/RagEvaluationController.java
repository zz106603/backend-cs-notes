package com.csnotes.rag.evaluation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag/evaluations")
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled", "rag.evaluation.enabled"}, havingValue = "true")
public class RagEvaluationController {
    private final RagEvaluationService service;

    public RagEvaluationController(RagEvaluationService service) {
        this.service = service;
    }

    @GetMapping
    public List<RagEvaluationCase> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RagEvaluationCase create(@RequestBody CreateRagEvaluationCaseRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/run")
    public RagEvaluationRunResponse run(@PathVariable UUID id) {
        return service.run(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
