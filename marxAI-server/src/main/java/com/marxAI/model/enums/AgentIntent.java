package com.marxAI.model.enums;

import java.util.Optional;

/**
 * High-level intent category for a user's message, determined by {@code IntentClassifier}.
 * Drives specialist-agent routing and scopes the Qdrant retrieval filter so that only
 * relevant note categories are searched for RAG context.
 */
public enum AgentIntent {

    /** Data Structures & Algorithms — LeetCode, complexity analysis, coding challenges. */
    DSA,

    /** Distributed systems, architecture, scalability, API design. */
    SYSTEM_DESIGN,

    /** Resume review, ATS scoring, job-application feedback. */
    RESUME,

    /** Simulated interview sessions with per-question evaluation. */
    MOCK_INTERVIEW,

    /** Adaptive day-by-day study plans based on weak topics and progress. */
    STUDY_PLAN,

    /** Anything that does not fit a specialist category. */
    GENERAL;

    /**
     * Returns the {@link DocumentType} used to scope Qdrant retrieval for this intent.
     * Returns {@link Optional#empty()} for intents that have no dedicated note category
     * (MOCK_INTERVIEW, STUDY_PLAN, GENERAL) so retrieval falls back to searching all documents.
     */
    public Optional<DocumentType> toDocumentType() {
        return switch (this) {
            case DSA -> Optional.of(DocumentType.DSA);
            case SYSTEM_DESIGN -> Optional.of(DocumentType.SYSTEM_DESIGN);
            case RESUME -> Optional.of(DocumentType.RESUME);
            default -> Optional.empty();
        };
    }
}
