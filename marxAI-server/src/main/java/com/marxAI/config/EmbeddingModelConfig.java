package com.marxAI.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link EmbeddingModel} bean ({@code EmbeddingService}'s only collaborator) from
 * {@link GeminiProperties}. Building the model doesn't make a network call — the API key is only
 * validated by Gemini when an embedding request actually goes out.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel(GeminiProperties properties) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.embeddingModel())
                .build();
    }
}
