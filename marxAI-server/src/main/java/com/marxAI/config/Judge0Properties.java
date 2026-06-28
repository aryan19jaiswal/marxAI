package com.marxAI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code judge0.*} keys from {@code application-dev.yml} used by {@code Judge0Config}
 * and {@code Judge0Client} to authenticate and talk to the Judge0 code-execution API.
 */
@ConfigurationProperties(prefix = "judge0")
public record Judge0Properties(
        /** Judge0 REST base URL, e.g. {@code https://judge0-ce.p.rapidapi.com}. */
        String baseUrl,
        /** RapidAPI key for Judge0 — sent as the {@code X-RapidAPI-Key} header. */
        String apiKey,
        /** Connect and read timeout in seconds for each Judge0 HTTP call (default: 15). */
        int timeoutSeconds) {}
