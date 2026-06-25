package com.marxAI.service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marxAI.exception.VectorStoreException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Embeds {@link TextChunk}s and keeps the {@code chunks} Postgres table in sync with the Qdrant
 * vector store: each chunk's vector lives in Qdrant (keyed by {@link Chunk#getQdrantId()}) while
 * its text and bookkeeping fields live in Postgres for traceability and source re-assembly.
 */
@Service
@RequiredArgsConstructor
public class QdrantService {

    static final String METADATA_DOCUMENT_ID = "documentId";
    static final String METADATA_PAGE_NUMBER = "pageNumber";
    static final String METADATA_DOC_TYPE = "docType";

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    /**
     * Embeds every chunk's text, upserts the vectors into Qdrant tagged with {@code documentId}/
     * {@code docType}/{@code pageNumber} metadata, then persists one {@link Chunk} row per {@code
     * textChunks} entry pointing back at its Qdrant point id.
     *
     * @param document owning document; supplies the {@code documentId}/{@code docType} metadata
     *     written alongside each vector
     * @param textChunks chunks produced by {@code ChunkingService}, in document order
     * @return the persisted {@link Chunk} entities, in the same order as {@code textChunks}; empty
     *     if {@code textChunks} is empty
     */
    public List<Chunk> upsertChunks(Document document, List<TextChunk> textChunks) {
        if (textChunks.isEmpty()) {
            return List.of();
        }

        List<TextSegment> segments = textChunks.stream()
                .map(textChunk -> TextSegment.from(textChunk.text(), buildMetadata(document, textChunk)))
                .toList();
        List<Embedding> embeddings = embeddingService.embedAll(segments);
        List<String> qdrantIds = embeddingStore.addAll(embeddings, segments);

        List<Chunk> chunks = new ArrayList<>(textChunks.size());
        for (int i = 0; i < textChunks.size(); i++) {
            TextChunk textChunk = textChunks.get(i);
            chunks.add(Chunk.builder()
                    .document(document)
                    .content(textChunk.text())
                    .qdrantId(qdrantIds.get(i))
                    .chunkIndex(textChunk.chunkIndex())
                    .metadata(toJson(Map.of("pageNumber", textChunk.pageNumber())))
                    .build());
        }
        return chunkRepository.saveAll(chunks);
    }

    /** Unfiltered top-{@code topK} similarity search; see {@link #similaritySearch(String, int, Filter)}. */
    public List<EmbeddingMatch<TextSegment>> similaritySearch(String query, int topK) {
        return similaritySearch(query, topK, null);
    }

    /**
     * @param query natural-language query to embed and search with
     * @param topK maximum number of matches to return
     * @param filter optional metadata filter (e.g. {@link #byDocumentType(DocumentType)}); {@code
     *     null} searches across every chunk regardless of metadata
     * @return matches ordered by descending cosine similarity
     */
    public List<EmbeddingMatch<TextSegment>> similaritySearch(String query, int topK, Filter filter) {
        Embedding queryEmbedding = embeddingService.embed(query);
        var requestBuilder =
                EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(topK);
        if (filter != null) {
            requestBuilder.filter(filter);
        }
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(requestBuilder.build());
        return result.matches();
    }

    /** Filter scoping a {@link #similaritySearch(String, int, Filter)} call to one knowledge-base category. */
    public static Filter byDocumentType(DocumentType docType) {
        return metadataKey(METADATA_DOC_TYPE).isEqualTo(docType.name());
    }

    /** Removes every chunk (Qdrant vectors + Postgres rows) belonging to {@code documentId}. */
    public void deleteByDocumentId(UUID documentId) {
        embeddingStore.removeAll(metadataKey(METADATA_DOCUMENT_ID).isEqualTo(documentId.toString()));
        chunkRepository.deleteByDocumentId(documentId);
    }

    private Metadata buildMetadata(Document document, TextChunk textChunk) {
        return new Metadata()
                .put(METADATA_DOCUMENT_ID, document.getId().toString())
                .put(METADATA_PAGE_NUMBER, textChunk.pageNumber())
                .put(METADATA_DOC_TYPE, document.getDocType());
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new VectorStoreException("Failed to serialize chunk metadata", ex);
        }
    }
}
