package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code gemini.*} keys from {@code application-dev.yml} used by {@code
 * EmbeddingModelConfig} to build the Gemini embedding client.
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String embeddingModel) {}
