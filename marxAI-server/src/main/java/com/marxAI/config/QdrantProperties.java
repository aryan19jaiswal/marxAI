package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code qdrant.*} keys from {@code application-dev.yml} used by {@code QdrantConfig}
 * (the gRPC port and collection that {@code QdrantService} reads/writes vectors against).
 */
@ConfigurationProperties(prefix = "qdrant")
public record QdrantProperties(String host, int grpcPort, String collectionName) {}
