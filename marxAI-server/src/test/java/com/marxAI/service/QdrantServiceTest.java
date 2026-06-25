package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marxAI.model.chunking.TextChunk;
import com.marxAI.model.entity.Chunk;
import com.marxAI.model.entity.Document;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.repository.ChunkRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link QdrantService}, with the {@link EmbeddingStore}, {@link EmbeddingService},
 * and {@link ChunkRepository} mocked (a real {@link ObjectMapper} is used since chunk metadata
 * serialization isn't worth mocking).
 */
@ExtendWith(MockitoExtension.class)
class QdrantServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChunkRepository chunkRepository;

    private QdrantService qdrantService;
    private Document document;

    @BeforeEach
    void setUp() {
        qdrantService = new QdrantService(embeddingStore, embeddingService, chunkRepository, new ObjectMapper());
        document = Document.builder().id(UUID.randomUUID()).docType("DSA").build();
    }

    @Test
    void upsertChunks_returnsEmptyList_withoutEmbeddingOrPersisting_whenTextChunksEmpty() {
        List<Chunk> result = qdrantService.upsertChunks(document, List.of());

        assertThat(result).isEmpty();
        verify(embeddingService, never()).embedAll(any());
        verify(chunkRepository, never()).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsertChunks_embedsSegmentsWithDocumentMetadata_thenPersistsChunksWithQdrantIds() {
        TextChunk chunkOne = new TextChunk(0, 1, "first chunk text");
        TextChunk chunkTwo = new TextChunk(1, 2, "second chunk text");
        Embedding embeddingOne = Embedding.from(new float[] {1f});
        Embedding embeddingTwo = Embedding.from(new float[] {2f});

        when(embeddingService.embedAll(any())).thenReturn(List.of(embeddingOne, embeddingTwo));
        when(embeddingStore.addAll(eq(List.of(embeddingOne, embeddingTwo)), any()))
                .thenReturn(List.of("qdrant-id-1", "qdrant-id-2"));
        when(chunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Chunk> result = qdrantService.upsertChunks(document, List.of(chunkOne, chunkTwo));

        ArgumentCaptor<List<TextSegment>> segmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingStore).addAll(eq(List.of(embeddingOne, embeddingTwo)), segmentsCaptor.capture());
        List<TextSegment> segments = segmentsCaptor.getValue();
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).text()).isEqualTo("first chunk text");
        Metadata metadata = segments.get(0).metadata();
        assertThat(metadata.getString("documentId")).isEqualTo(document.getId().toString());
        assertThat(metadata.getString("docType")).isEqualTo("DSA");
        assertThat(metadata.getInteger("pageNumber")).isEqualTo(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDocument()).isSameAs(document);
        assertThat(result.get(0).getContent()).isEqualTo("first chunk text");
        assertThat(result.get(0).getQdrantId()).isEqualTo("qdrant-id-1");
        assertThat(result.get(0).getChunkIndex()).isZero();
        assertThat(result.get(0).getMetadata()).contains("\"pageNumber\":1");
        assertThat(result.get(1).getQdrantId()).isEqualTo("qdrant-id-2");
        assertThat(result.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(result.get(1).getMetadata()).contains("\"pageNumber\":2");
    }

    @Test
    void similaritySearch_embedsQueryAndSearchesWithoutFilter_whenFilterOmitted() {
        Embedding queryEmbedding = Embedding.from(new float[] {0.5f});
        when(embeddingService.embed("binary trees")).thenReturn(queryEmbedding);
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id", queryEmbedding, TextSegment.from("hit"));
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<EmbeddingMatch<TextSegment>> results = qdrantService.similaritySearch("binary trees", 5);

        ArgumentCaptor<EmbeddingSearchRequest> requestCaptor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queryEmbedding()).isSameAs(queryEmbedding);
        assertThat(requestCaptor.getValue().maxResults()).isEqualTo(5);
        assertThat(requestCaptor.getValue().filter()).isNull();
        assertThat(results).containsExactly(match);
    }

    @Test
    void similaritySearch_appliesFilter_whenProvided() {
        Embedding queryEmbedding = Embedding.from(new float[] {0.5f});
        when(embeddingService.embed("binary trees")).thenReturn(queryEmbedding);
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of()));
        Filter filter = QdrantService.byDocumentType(DocumentType.DSA);

        qdrantService.similaritySearch("binary trees", 3, filter);

        ArgumentCaptor<EmbeddingSearchRequest> requestCaptor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().filter()).isEqualTo(filter);
    }

    @Test
    void byDocumentType_buildsEqualToFilter_onDocTypeMetadataKey() {
        Filter filter = QdrantService.byDocumentType(DocumentType.SYSTEM_DESIGN);

        assertThat(filter).isEqualTo(new IsEqualTo("docType", "SYSTEM_DESIGN"));
    }

    @Test
    void deleteByDocumentId_removesVectorsByDocumentIdFilter_andDeletesChunkRows() {
        UUID documentId = UUID.randomUUID();

        qdrantService.deleteByDocumentId(documentId);

        verify(embeddingStore).removeAll(new IsEqualTo("documentId", documentId.toString()));
        verify(chunkRepository).deleteByDocumentId(documentId);
    }
}
