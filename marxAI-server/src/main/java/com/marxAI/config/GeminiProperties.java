package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code gemini.*} keys from {@code application-dev.yml} used by both the embedding
 * client and the chat language model beans.
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
    String apiKey,
    String embeddingModel,
    String chatModel,
    String chatModelFast) {}
