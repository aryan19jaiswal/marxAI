package com.marxAI.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link EmbeddingModel} bean ({@code EmbeddingService}'s only collaborator) from
 * {@link OpenAiProperties}. Building the model doesn't make a network call — the API key is only
 * validated by OpenAI when an embedding request actually goes out.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel(OpenAiProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.embeddingModel())
                .build();
    }
}
