package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marxAI.model.dto.AssembledContext;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.repository.ChunkRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration test for the full RAG retrieval pipeline:
 * {@link RetrievalService} → {@link QdrantService} → {@link ContextAssembler}.
 *
 * <p>The {@link EmbeddingStore} and {@link EmbeddingService} are mocked so the test runs without a
 * live Qdrant instance or Gemini API key, while still exercising the real wiring of all three
 * service classes together. This matches the Day-12 requirement: query "binary search" →
 * returns assembled context from DSA-tagged chunks.
 */
@ExtendWith(MockitoExtension.class)
class RagRetrievalIntegrationTest {

    private static final String BINARY_SEARCH_TEXT =
            "Binary search is an algorithm that finds the position of a target value "
                    + "within a sorted array. It runs in O(log n) time by halving the "
                    + "search space on each iteration.";

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChunkRepository chunkRepository;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        QdrantService qdrantService =
                new QdrantService(embeddingStore, embeddingService, chunkRepository, new ObjectMapper());
        ContextAssembler contextAssembler = new ContextAssembler();
        retrievalService = new RetrievalService(qdrantService, contextAssembler);
    }

    @Test
    void retrieve_binarySearchQueryWithDsaFilter_returnsContextContainingDsaChunk() {
        Embedding fakeEmbedding = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
        when(embeddingService.embed("binary search")).thenReturn(fakeEmbedding);

        Metadata metadata = new Metadata()
                .put("documentId", "doc-dsa-1")
                .put("docType", DocumentType.DSA.name())
                .put("pageNumber", 1);
        TextSegment dsaChunk = TextSegment.from(BINARY_SEARCH_TEXT, metadata);
        EmbeddingMatch<TextSegment> dsaMatch =
                new EmbeddingMatch<>(0.92, "vec-id-1", fakeEmbedding, dsaChunk);
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(List.of(dsaMatch)));

        AssembledContext result = retrievalService.retrieve("binary search", DocumentType.DSA);

        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.context()).startsWith("### Source 1:");
        assertThat(result.context()).contains("Binary search");
        assertThat(result.context()).contains("O(log n)");
    }

    @Test
    void retrieve_multipleDistinctChunks_assemblesAllInOrder() {
        Embedding fakeEmbedding = Embedding.from(new float[]{0.5f});
        when(embeddingService.embed("sorting algorithms")).thenReturn(fakeEmbedding);

        TextSegment chunk1 = TextSegment.from("Merge sort is O(n log n) with O(n) extra space.");
        TextSegment chunk2 = TextSegment.from("Quick sort is O(n log n) average, O(n^2) worst case.");
        List<EmbeddingMatch<TextSegment>> matches = List.of(
                new EmbeddingMatch<>(0.91, "id-1", fakeEmbedding, chunk1),
                new EmbeddingMatch<>(0.88, "id-2", fakeEmbedding, chunk2));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(matches));

        AssembledContext result = retrievalService.retrieve("sorting algorithms");

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.context()).contains("### Source 1:");
        assertThat(result.context()).contains("### Source 2:");
        assertThat(result.context()).contains("Merge sort");
        assertThat(result.context()).contains("Quick sort");
    }

    @Test
    void retrieve_duplicateChunks_deduplicatesAndReturnsSingleSource() {
        Embedding fakeEmbedding = Embedding.from(new float[]{0.5f});
        when(embeddingService.embed("binary search")).thenReturn(fakeEmbedding);

        TextSegment chunk = TextSegment.from(BINARY_SEARCH_TEXT);
        EmbeddingMatch<TextSegment> first = new EmbeddingMatch<>(0.95, "id-1", fakeEmbedding, chunk);
        EmbeddingMatch<TextSegment> dupe = new EmbeddingMatch<>(0.88, "id-2", fakeEmbedding, chunk);
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(List.of(first, dupe)));

        AssembledContext result = retrievalService.retrieve("binary search", DocumentType.DSA);

        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.context()).doesNotContain("### Source 2:");
    }

    @Test
    void retrieve_noMatchesFromQdrant_returnsEmptyContext() {
        Embedding fakeEmbedding = Embedding.from(new float[]{0.5f});
        when(embeddingService.embed("nonexistent topic")).thenReturn(fakeEmbedding);
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));

        AssembledContext result = retrievalService.retrieve("nonexistent topic", DocumentType.DSA);

        assertThat(result.context()).isEmpty();
        assertThat(result.sourceCount()).isZero();
    }
}
