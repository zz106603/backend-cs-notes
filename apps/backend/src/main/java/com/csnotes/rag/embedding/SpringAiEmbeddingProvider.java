package com.csnotes.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.stream.IntStream;

/** Spring AI 모델 응답을 애플리케이션의 임베딩 포트 모델로 변환한다. */
public final class SpringAiEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingModel embeddingModel;
    private final String modelName;
    private final int dimensions;

    public SpringAiEmbeddingProvider(EmbeddingModel embeddingModel, String modelName, int dimensions) {
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
        this.dimensions = dimensions;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public List<EmbeddingVector> embed(List<EmbeddingInput> inputs, EmbeddingPurpose purpose) {
        // Spring AI 공통 API에는 provider별 input type이 없으므로 purpose는 향후 모델별 어댑터에서 사용한다.
        var response = embeddingModel.embedForResponse(inputs.stream().map(EmbeddingInput::text).toList());
        return IntStream.range(0, response.getResults().size())
                .mapToObj(index -> new EmbeddingVector(
                        inputs.get(index).id(), modelName, response.getResults().get(index).getOutput()
                ))
                .toList();
    }
}
