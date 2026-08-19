package com.csnotes.rag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Spring AI 모델 응답을 애플리케이션의 임베딩 포트 모델로 변환한다. */
public final class SpringAiEmbeddingProvider implements EmbeddingProvider {
    private static final Logger log = LoggerFactory.getLogger(SpringAiEmbeddingProvider.class);

    private final EmbeddingModel embeddingModel;
    private final String modelName;
    private final int dimensions;
    private final int batchSize;

    public SpringAiEmbeddingProvider(EmbeddingModel embeddingModel, String modelName, int dimensions) {
        this(embeddingModel, modelName, dimensions, 64);
    }

    public SpringAiEmbeddingProvider(EmbeddingModel embeddingModel, String modelName, int dimensions, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Embedding batch size must be greater than zero");
        }
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.batchSize = batchSize;
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
        if (inputs.isEmpty()) {
            return List.of();
        }

        long startedAt = System.nanoTime();
        List<EmbeddingVector> vectors = new ArrayList<>(inputs.size());
        int batchCount = (inputs.size() + batchSize - 1) / batchSize;

        try {
            // OpenAI 요청 크기와 장애 영향 범위를 제한하면서 입력 순서를 그대로 보존한다.
            for (int start = 0; start < inputs.size(); start += batchSize) {
                List<EmbeddingInput> batch = inputs.subList(start, Math.min(start + batchSize, inputs.size()));
                var response = embeddingModel.embedForResponse(batch.stream().map(EmbeddingInput::text).toList());
                if (response.getResults().size() != batch.size()) {
                    throw new EmbeddingProviderException("Embedding model returned an unexpected number of vectors");
                }
                for (int index = 0; index < batch.size(); index++) {
                    vectors.add(new EmbeddingVector(
                            batch.get(index).id(), modelName, response.getResults().get(index).getOutput()
                    ));
                }
            }
            log.info("임베딩 완료: purpose={}, model={}, inputs={}, batches={}, elapsedMs={}",
                    purpose, modelName, inputs.size(), batchCount, elapsedMillis(startedAt));
            return List.copyOf(vectors);
        } catch (EmbeddingProviderException exception) {
            log.warn("임베딩 실패: purpose={}, model={}, inputs={}, batches={}, elapsedMs={}",
                    purpose, modelName, inputs.size(), batchCount, elapsedMillis(startedAt));
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("임베딩 API 호출 실패: purpose={}, model={}, inputs={}, batches={}, elapsedMs={}, cause={}",
                    purpose, modelName, inputs.size(), batchCount, elapsedMillis(startedAt),
                    exception.getClass().getSimpleName());
            throw new EmbeddingProviderException("Embedding API request failed", exception);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
