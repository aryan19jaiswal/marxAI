package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code openai.*} keys from {@code application-dev.yml} used by {@code
 * EmbeddingModelConfig} to build the OpenAI embedding client.
 */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String embeddingModel) {}
