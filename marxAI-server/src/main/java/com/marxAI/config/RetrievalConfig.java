package com.marxAI.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires LangChain4J's {@link EmbeddingStoreContentRetriever} as a Spring bean so it can be
 * injected into agent {@code AiServices} that need RAG context.
 *
 * <p>This retriever is unfiltered (searches all categories). Agents that need a
 * category-scoped retriever (e.g. DSA-only) should build one inline via
 * {@link EmbeddingStoreContentRetriever#builder()} with a {@code dynamicFilter},
 * or call {@link com.marxAI.service.RetrievalService#retrieve(String,
 * com.marxAI.model.enums.DocumentType)} instead.
 */
@Configuration
public class RetrievalConfig {

    /**
     * Default {@link ContentRetriever} backed by the Qdrant embedding store and Gemini
     * {@code text-embedding-004} model. Returns up to 5 chunks per query with a 0.6 minimum
     * cosine-similarity score.
     *
     * @param embeddingStore Qdrant-backed store configured by {@link QdrantConfig}
     * @param embeddingModel Gemini embedding model configured by {@link EmbeddingModelConfig}
     * @return default content retriever for unfiltered RAG queries
     */
    @Bean
    public ContentRetriever embeddingStoreContentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }
}
