package com.marxAI.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the {@link EmbeddingModel} bean (Gemini {@code text-embedding-004}, see
 * {@code EmbeddingModelConfig}), giving {@code QdrantService} a single place to call the
 * embedding API from.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Embeds a single piece of text, e.g. a similarity-search query.
     *
     * @param text the text to embed
     * @return the resulting vector
     */
    public Embedding embed(String text) {
        return embeddingModel.embed(text).content();
    }

    /**
     * Embeds a batch of segments in one API call. Segments (rather than plain strings) are taken
     * so the same list can be reused unchanged when writing to the {@code EmbeddingStore}
     * afterwards; the embedding call itself only reads {@link TextSegment#text()} and ignores
     * metadata.
     *
     * @param segments segments to embed, in order
     * @return one {@link Embedding} per segment, same order as {@code segments}; empty if {@code
     *     segments} is empty
     */
    public List<Embedding> embedAll(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }
        return embeddingModel.embedAll(segments).content();
    }
}
