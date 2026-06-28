package com.marxAI.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Wires the Judge0 code-execution infrastructure: properties binding and a
 * pre-configured {@link RestTemplate} used by {@code Judge0Client}.
 *
 * <p>Judge0 is accessed via RapidAPI, so every request needs the
 * {@code X-RapidAPI-Key} and {@code X-RapidAPI-Host} headers attached by default.
 * Using a dedicated {@link RestTemplate} bean keeps those headers out of application code.
 */
@Configuration
@EnableConfigurationProperties(Judge0Properties.class)
public class Judge0Config {

    /**
     * {@link RestTemplate} dedicated to Judge0 REST calls.
     *
     * <p>Pre-configured with:
     * <ul>
     *   <li>Root URI from {@code judge0.base-url} so callers use relative paths.</li>
     *   <li>Connect and read timeouts driven by {@code judge0.timeout-seconds}.</li>
     *   <li>Default authentication headers required by the RapidAPI gateway.</li>
     * </ul>
     *
     * Qualified as {@code "judge0RestTemplate"} to avoid ambiguity with any other
     * {@link RestTemplate} beans that might be present.
     */
    @Bean
    @Qualifier("judge0RestTemplate")
    public RestTemplate judge0RestTemplate(RestTemplateBuilder builder, Judge0Properties props) {
        Duration timeout = Duration.ofSeconds(props.timeoutSeconds());
        return builder
                .rootUri(props.baseUrl())
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-RapidAPI-Key", props.apiKey())
                .defaultHeader("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com")
                .build();
    }
}
