package com.csnotes.rag.answer;

import com.csnotes.rag.embedding.OpenAiApiKeyCondition;
import com.csnotes.rag.search.RagSearchService;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;

@Configuration
@Conditional(OpenAiApiKeyCondition.class)
@ConditionalOnProperty(name = {"rag.persistence.enabled", "rag.search.enabled", "rag.answer.enabled"}, havingValue = "true")
public class RagAnswerConfiguration {
    /** 답변 기능이 명시적으로 켜진 경우에만 비용이 발생할 수 있는 ChatModel을 구성한다. */
    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    ChatModel openAiAnswerChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${rag.answer.model}") String modelName,
            @Value("${rag.answer.max-output-tokens}") int maxOutputTokens,
            @Value("${rag.answer.temperature}") double temperature,
            @Value("${spring.ai.retry.max-attempts:3}") int maxAttempts,
            @Value("${spring.ai.retry.backoff.initial-interval:1s}") Duration initialInterval,
            @Value("${spring.ai.retry.backoff.multiplier:2}") double multiplier,
            @Value("${spring.ai.retry.backoff.max-interval:10s}") Duration maxInterval
    ) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .maxCompletionTokens(maxOutputTokens)
                .temperature(temperature)
                .store(false)
                .build();
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(initialInterval, multiplier, maxInterval)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(options)
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(retryTemplate)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    @Bean
    RagAnswerGenerator ragAnswerGenerator(
            ChatModel chatModel,
            @Value("${rag.answer.model}") String modelName
    ) {
        return new SpringAiRagAnswerGenerator(chatModel, modelName);
    }

    @Bean
    RagAnswerService ragAnswerService(
            RagSearchService searchService,
            RagAnswerGenerator answerGenerator,
            @Value("${rag.answer.default-source-limit}") int defaultSourceLimit,
            @Value("${rag.answer.max-source-limit}") int maxSourceLimit,
            @Value("${rag.answer.max-context-characters}") int maxContextCharacters,
            @Value("${rag.answer.default-minimum-score}") double defaultMinimumScore,
            @Value("${rag.answer.cache-ttl}") Duration cacheTtl,
            @Value("${rag.answer.cache-max-entries}") int cacheMaxEntries
    ) {
        return new RagAnswerService(searchService, answerGenerator, defaultSourceLimit, maxSourceLimit,
                maxContextCharacters, defaultMinimumScore, cacheTtl, cacheMaxEntries);
    }
}
