package com.marxAI.model.dto;

/**
 * Result of {@link com.marxAI.service.ContextAssembler#assemble}: a formatted context string
 * ready for injection into an LLM prompt and a count of unique source chunks it contains.
 *
 * @param context formatted source blocks, e.g. {@code "### Source 1:\n...\n### Source 2:\n..."},
 *     or an empty string when no chunks were provided or all exceeded the character limit
 * @param sourceCount number of unique source chunks included in {@code context}; zero when
 *     {@code context} is empty
 */
public record AssembledContext(String context, int sourceCount) {

    /** Sentinel for callers that need an empty result without null-checking. */
    public static AssembledContext empty() {
        return new AssembledContext("", 0);
    }
}
