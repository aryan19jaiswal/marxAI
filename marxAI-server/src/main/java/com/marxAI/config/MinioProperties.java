package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code minio.*} keys from {@code application-dev.yml} (endpoint URL, credentials, and
 * the bucket used for all user-uploaded documents).
 */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(String url, String accessKey, String secretKey, String bucket) {}
