package com.marxAI.model.enums;

/**
 * Knowledge-base category a user assigns to an uploaded {@link com.marxAI.model.entity.Document}.
 * Persisted as its {@link #name()} in {@code documents.doc_type} and later used to scope Qdrant
 * similarity search (e.g. restrict retrieval to {@code DSA} notes for a DSA chat session).
 */
public enum DocumentType {
    DSA,
    SYSTEM_DESIGN,
    RESUME
}
