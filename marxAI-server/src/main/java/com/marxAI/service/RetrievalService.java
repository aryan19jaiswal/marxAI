package com.marxAI.service;

import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.enums.DocumentType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates RAG retrieval: queries Qdrant for the most semantically similar chunks to the
 * user's query (optionally scoped to a knowledge-base category), then delegates to
 * {@link ContextAssembler} to produce a deduplicated, truncated, prompt-ready context string.
 *
 * <p>This is the primary entry point for agents that need to inject document context into their
 * prompts. Use {@link #retrieve(String)} for unfiltered retrieval across all documents, or
 * {@link #retrieve(String, DocumentType)} to restrict results to a single category (e.g. DSA-only
 * for the DSA agent, SYSTEM_DESIGN-only for the system design agent).
 */
@Service
@RequiredArgsConstructor
public class RetrievalService {

    /** Default number of candidate chunks to fetch from Qdrant before deduplication. */
    static final int DEFAULT_TOP_K = 5;

    private final QdrantService qdrantService;
    private final ContextAssembler contextAssembler;

    /**
     * Retrieves context for {@code query} across all document types using
     * {@value #DEFAULT_TOP_K} candidates.
     *
     * @param query the user's natural-language query
     * @return assembled context ready for LLM prompt injection
     */
    public AssembledContext retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K, null);
    }

    /**
     * Retrieves context for {@code query} scoped to {@code docType} using
     * {@value #DEFAULT_TOP_K} candidates.
     *
     * @param query   the user's natural-language query
     * @param docType knowledge-base category to restrict retrieval to; {@code null} searches all
     * @return assembled context ready for LLM prompt injection
     */
    public AssembledContext retrieve(String query, DocumentType docType) {
        return retrieve(query, DEFAULT_TOP_K, docType);
    }

    /**
     * Full retrieval with explicit candidate count and optional category filter.
     *
     * @param query   the user's natural-language query
     * @param topK    maximum number of candidate chunks to retrieve from Qdrant before
     *                deduplication and truncation
     * @param docType knowledge-base category filter; {@code null} searches all categories
     * @return assembled context ready for LLM prompt injection
     */
    public AssembledContext retrieve(String query, int topK, DocumentType docType) {
        Filter filter = docType != null ? QdrantService.byDocumentType(docType) : null;
        List<EmbeddingMatch<TextSegment>> matches = qdrantService.similaritySearch(query, topK, filter);
        return contextAssembler.assemble(matches);
    }
}
