package com.marxAI.model.dto;

import com.marxAI.model.enums.AgentIntent;

/**
 * Structured output produced by {@link com.marxAI.agent.IntentClassifier}.
 * LangChain4J AiServices generates a JSON schema from this record type, instructs the model
 * to respond in that format, and deserialises the result automatically via Jackson.
 *
 * @param intent     classified intent category; drives agent routing and RAG filter scoping
 * @param confidence model's self-reported confidence in [0.0, 1.0]
 * @param topic      primary topic extracted from the message (e.g. "binary search", "URL shortener")
 * @param difficulty estimated difficulty — "easy", "medium", "hard", or "n/a" when not applicable
 * @param entities   comma-separated key technical terms (e.g. "binary tree, recursion, BFS")
 */
public record IntentClassificationResult(
        AgentIntent intent,
        double confidence,
        String topic,
        String difficulty,
        String entities) {}
